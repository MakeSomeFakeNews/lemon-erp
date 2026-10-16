package com.lemon.erp.framework.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;

/**
 * 统一日期时间的 JSON 格式：
 * <ul>
 *   <li>LocalDateTime 输出 yyyy-MM-dd HH:mm:ss；读取时兼容 yyyy-MM-dd HH:mm[:ss] 和 ISO 格式 yyyy-MM-ddTHH:mm[:ss][.SSS]</li>
 *   <li>java.util.Date（Mapper 返回 Map 时 DATETIME / TIMESTAMP 列可能是 java.sql.Timestamp）按服务器时区输出 yyyy-MM-dd HH:mm:ss，
 *       java.sql.Date 输出 yyyy-MM-dd，不再输出 2026-10-15T07:00:00.000Z 这样的 UTC 时间</li>
 * </ul>
 * 字段上的 @JsonFormat 仍然优先
 */
@Configuration
public class JacksonConfig {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 日期和时间之间可以是空格或 T，秒和毫秒可选 */
    private static final DateTimeFormatter LENIENT_DATE_TIME = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .optionalStart().appendLiteral(' ').optionalEnd()
            .optionalStart().appendLiteral('T').optionalEnd()
            .appendPattern("HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
            .toFormatter();

    @Bean
    public JsonMapperBuilderCustomizer dateTimeFormatCustomizer() {
        SimpleModule module = new SimpleModule("lemon-date-time");
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(LENIENT_DATE_TIME));
        module.addSerializer(Date.class, new DateSerializer());
        return builder -> builder.addModule(module);
    }

    /** java.util.Date 及其 java.sql 子类 */
    static class DateSerializer extends ValueSerializer<Date> {

        @Override
        public void serialize(Date value, JsonGenerator gen, SerializationContext ctxt) {
            if (value instanceof java.sql.Date d) {
                gen.writeString(d.toLocalDate().toString());
            } else if (value instanceof java.sql.Time t) {
                gen.writeString(t.toLocalTime().toString());
            } else {
                gen.writeString(DATE_TIME.format(value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
            }
        }
    }
}
