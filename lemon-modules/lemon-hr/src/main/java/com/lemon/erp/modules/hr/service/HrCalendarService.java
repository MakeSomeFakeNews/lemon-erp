package com.lemon.erp.modules.hr.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import com.lemon.erp.modules.hr.entity.HrHoliday;
import com.lemon.erp.modules.hr.entity.HrShift;
import com.lemon.erp.modules.hr.mapper.HrEmployeeMapper;
import com.lemon.erp.modules.hr.mapper.HrHolidayMapper;
import com.lemon.erp.modules.hr.mapper.HrShiftMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工作日历与班次：工作日 = 周一至周五 + 调休上班 - 法定节假日；员工未指定班次时使用默认班次
 */
@Service
@RequiredArgsConstructor
public class HrCalendarService {

    /** 日期类型，决定加班费倍数 */
    public enum DayType { WORKDAY, RESTDAY, HOLIDAY }

    private final HrShiftMapper shiftMapper;
    private final HrHolidayMapper holidayMapper;
    private final HrEmployeeMapper employeeMapper;

    // ---------------------------------------------------------------- 班次

    public List<HrShift> shifts() {
        return shiftMapper.selectList(Wrappers.<HrShift>lambdaQuery().orderByDesc(HrShift::getIsDefault).orderByAsc(HrShift::getId));
    }

    @Transactional
    public void saveShift(Long id, HrShift shift) {
        if (!shift.getEndTime().isAfter(shift.getStartTime())) {
            throw new BizException("下班时间必须晚于上班时间（暂不支持跨天班次）");
        }
        if (shift.getFlexMinutes() == null) shift.setFlexMinutes(0);
        if (shift.getWorkHours() == null) shift.setWorkHours(BigDecimal.valueOf(8));
        boolean makeDefault = Integer.valueOf(1).equals(shift.getIsDefault()) || !shiftMapper.exists(Wrappers.emptyWrapper());
        shift.setIsDefault(makeDefault ? 1 : 0);
        if (id == null) {
            shift.setId(null);
            shiftMapper.insert(shift);
        } else {
            HrShift old = shiftMapper.selectById(id);
            if (old == null) {
                throw new BizException("班次不存在");
            }
            if (Integer.valueOf(1).equals(old.getIsDefault())) {
                shift.setIsDefault(1);
            }
            shift.setId(id);
            shiftMapper.updateById(shift);
        }
        if (makeDefault) {
            shiftMapper.update(Wrappers.<HrShift>lambdaUpdate().set(HrShift::getIsDefault, 0).ne(HrShift::getId, shift.getId()));
        }
    }

    public void deleteShift(Long id) {
        HrShift shift = shiftMapper.selectById(id);
        if (shift == null) {
            return;
        }
        if (Integer.valueOf(1).equals(shift.getIsDefault())) {
            throw new BizException("默认班次不能删除，请先把其他班次设为默认");
        }
        if (employeeMapper.exists(Wrappers.<HrEmployee>lambdaQuery().eq(HrEmployee::getShiftId, id))) {
            throw new BizException("班次「" + shift.getName() + "」已分配给员工，不能删除");
        }
        shiftMapper.deleteById(id);
    }

    /** 默认班次；没有配置时按 9:00-18:00 */
    public HrShift defaultShift() {
        HrShift shift = shiftMapper.selectList(Wrappers.<HrShift>lambdaQuery().eq(HrShift::getIsDefault, 1)).stream().findFirst().orElse(null);
        if (shift == null) {
            shift = new HrShift();
            shift.setName("标准班");
            shift.setStartTime(LocalTime.of(9, 0));
            shift.setEndTime(LocalTime.of(18, 0));
            shift.setFlexMinutes(0);
            shift.setWorkHours(BigDecimal.valueOf(8));
        }
        return shift;
    }

    /** 员工的班次 */
    public HrShift shiftOf(HrEmployee e) {
        HrShift shift = e.getShiftId() == null ? null : shiftMapper.selectById(e.getShiftId());
        return shift != null ? shift : defaultShift();
    }

    /** 全部班次 ID → 班次，批量计算时使用 */
    Map<Long, HrShift> shiftMap() {
        return shifts().stream().collect(Collectors.toMap(HrShift::getId, Function.identity()));
    }

    // ---------------------------------------------------------------- 节假日

    public List<HrHoliday> holidays(int year) {
        return holidayMapper.selectList(Wrappers.<HrHoliday>lambdaQuery()
                .between(HrHoliday::getHolidayDate, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .orderByAsc(HrHoliday::getHolidayDate));
    }

    public void saveHoliday(Long id, HrHoliday holiday) {
        HrHoliday exist = holidayMapper.selectOne(Wrappers.<HrHoliday>lambdaQuery().eq(HrHoliday::getHolidayDate, holiday.getHolidayDate()));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException(holiday.getHolidayDate() + " 已设置为「" + exist.getName() + "」");
        }
        holiday.setId(id);
        if (id == null) {
            holidayMapper.insert(holiday);
        } else {
            holidayMapper.updateById(holiday);
        }
    }

    public void deleteHoliday(List<Long> ids) {
        holidayMapper.deleteByIds(ids);
    }

    /** 区间内设置过的节假日 / 调休上班日 */
    public Map<LocalDate, HrHoliday> holidayMap(LocalDate from, LocalDate to) {
        return holidayMapper.selectList(Wrappers.<HrHoliday>lambdaQuery().between(HrHoliday::getHolidayDate, from, to))
                .stream().collect(Collectors.toMap(HrHoliday::getHolidayDate, Function.identity()));
    }

    public static boolean isWorkday(LocalDate date, Map<LocalDate, HrHoliday> holidays) {
        HrHoliday h = holidays.get(date);
        if (h != null) {
            return "WORKDAY".equals(h.getType());
        }
        return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    public static DayType dayType(LocalDate date, Map<LocalDate, HrHoliday> holidays) {
        HrHoliday h = holidays.get(date);
        if (h != null && "HOLIDAY".equals(h.getType())) {
            return DayType.HOLIDAY;
        }
        return isWorkday(date, holidays) ? DayType.WORKDAY : DayType.RESTDAY;
    }

    /** [from, to] 内的工作日；from 晚于 to 时为空 */
    public static List<LocalDate> workdays(LocalDate from, LocalDate to, Map<LocalDate, HrHoliday> holidays) {
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (isWorkday(d, holidays)) {
                days.add(d);
            }
        }
        return days;
    }

    public boolean isWorkday(LocalDate date) {
        return isWorkday(date, holidayMap(date, date));
    }
}
