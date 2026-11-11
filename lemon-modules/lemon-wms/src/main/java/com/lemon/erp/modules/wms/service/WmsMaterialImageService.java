package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.file.FileStorageService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsMaterialImage;
import com.lemon.erp.modules.wms.mapper.WmsMaterialImageMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 物料（商品）图片：上传、排序、设主图、删除。
 * <ul>
 *   <li>图片保存在公开文件目录（{@link FileStorageService#storeImage}，与头像相同），URL 随机、通过 /api/files/** 直接访问，
 *       供电商小程序等直接展示；图片列表、维护接口按物料权限校验</li>
 *   <li>排序最小的为主图，主图 URL 同步到物料的 image_url，列表、下拉、库存查询据此显示缩略图</li>
 *   <li>文件在事务提交后删除，上传后事务回滚时删除已保存的文件</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class WmsMaterialImageService {

    /** 公开文件目录下的业务目录 */
    static final String DIR = "material";
    /** 每个物料最多的图片数 */
    public static final int MAX_IMAGES = 10;
    /** 单张图片最大字节数 */
    public static final long MAX_SIZE = 5L * 1024 * 1024;

    private final WmsMaterialImageMapper imageMapper;
    private final WmsMaterialMapper materialMapper;
    private final FileStorageService fileStorageService;

    /** 物料的图片，第一张为主图 */
    public List<WmsMaterialImage> list(Long materialId) {
        return imageMapper.selectList(Wrappers.<WmsMaterialImage>lambdaQuery()
                .eq(WmsMaterialImage::getMaterialId, materialId)
                .orderByAsc(WmsMaterialImage::getSort).orderByAsc(WmsMaterialImage::getId));
    }

    /**
     * 多个物料的图片 URL（第一张为主图），供电商小程序等展示商品图
     *
     * @return 物料 ID → URL 列表，没有图片的物料不出现
     */
    public Map<Long, List<String>> urls(Collection<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return Map.of();
        }
        if (materialIds.size() > 500) {
            throw new BizException("wms.error.imageBatchTooMany", 500);
        }
        return imageMapper.selectList(Wrappers.<WmsMaterialImage>lambdaQuery()
                        .select(WmsMaterialImage::getMaterialId, WmsMaterialImage::getUrl)
                        .in(WmsMaterialImage::getMaterialId, materialIds)
                        .orderByAsc(WmsMaterialImage::getMaterialId).orderByAsc(WmsMaterialImage::getSort).orderByAsc(WmsMaterialImage::getId))
                .stream().collect(Collectors.groupingBy(WmsMaterialImage::getMaterialId, LinkedHashMap::new,
                        Collectors.mapping(WmsMaterialImage::getUrl, Collectors.toList())));
    }

    /** 上传一张图片，排在最后（没有图片时成为主图） */
    @Transactional
    public WmsMaterialImage upload(Long materialId, MultipartFile file) {
        requireMaterial(materialId);
        List<WmsMaterialImage> images = list(materialId);
        if (images.size() >= MAX_IMAGES) {
            throw new BizException("wms.error.imageTooMany", MAX_IMAGES);
        }
        String url = fileStorageService.storeImage(file, DIR, MAX_SIZE);
        deleteOnRollback(url);
        WmsMaterialImage image = new WmsMaterialImage();
        image.setMaterialId(materialId);
        image.setUrl(url);
        image.setFileName(fileName(file.getOriginalFilename()));
        image.setFileSize(file.getSize());
        image.setSort(images.isEmpty() ? 1 : images.get(images.size() - 1).getSort() + 1);
        imageMapper.insert(image);
        if (images.isEmpty()) {
            syncMain(materialId, url);
        }
        return image;
    }

    /**
     * 调整顺序：按传入的图片 ID 顺序排序，第一张为主图
     *
     * @param imageIds 该物料的全部图片 ID
     */
    @Transactional
    public void reorder(Long materialId, List<Long> imageIds) {
        requireMaterial(materialId);
        List<WmsMaterialImage> images = list(materialId);
        Set<Long> existing = images.stream().map(WmsMaterialImage::getId).collect(Collectors.toSet());
        if (imageIds == null || imageIds.size() != existing.size() || !existing.equals(new HashSet<>(imageIds))) {
            throw new BizException("wms.error.imageListChanged");
        }
        for (int i = 0; i < imageIds.size(); i++) {
            WmsMaterialImage update = new WmsMaterialImage();
            update.setId(imageIds.get(i));
            update.setSort(i + 1);
            imageMapper.updateById(update);
        }
        String mainUrl = images.stream().filter(img -> img.getId().equals(imageIds.get(0))).findFirst()
                .map(WmsMaterialImage::getUrl).orElse(null);
        syncMain(materialId, mainUrl);
    }

    /** 设为主图：移到第一张，其余顺序不变 */
    @Transactional
    public void setMain(Long materialId, Long imageId) {
        List<Long> ids = new ArrayList<>(list(materialId).stream().map(WmsMaterialImage::getId).toList());
        if (!ids.remove(imageId)) {
            throw new BizException("wms.error.imageNotFound");
        }
        ids.add(0, imageId);
        reorder(materialId, ids);
    }

    @Transactional
    public void delete(Long materialId, Long imageId) {
        requireMaterial(materialId);
        WmsMaterialImage image = imageMapper.selectById(imageId);
        if (image == null || !image.getMaterialId().equals(materialId)) {
            throw new BizException("wms.error.imageNotFound");
        }
        imageMapper.deleteById(imageId);
        deleteAfterCommit(List.of(image.getUrl()));
        List<WmsMaterialImage> rest = list(materialId);
        syncMain(materialId, rest.isEmpty() ? null : rest.get(0).getUrl());
    }

    /** 删除物料时删除其全部图片（在删除物料的事务中调用） */
    void deleteByMaterials(Collection<Long> materialIds) {
        List<WmsMaterialImage> images = imageMapper.selectList(Wrappers.<WmsMaterialImage>lambdaQuery()
                .select(WmsMaterialImage::getId, WmsMaterialImage::getUrl)
                .in(WmsMaterialImage::getMaterialId, materialIds));
        if (images.isEmpty()) {
            return;
        }
        imageMapper.delete(Wrappers.<WmsMaterialImage>lambdaQuery().in(WmsMaterialImage::getMaterialId, materialIds));
        deleteAfterCommit(images.stream().map(WmsMaterialImage::getUrl).toList());
    }

    /** 复制物料时复制图片（复制文件，两个物料的图片互不影响），在新增物料的事务中调用 */
    void copy(Long fromMaterialId, Long toMaterialId) {
        String main = null;
        int sort = 0;
        for (WmsMaterialImage source : list(fromMaterialId)) {
            String url = fileStorageService.copyImage(source.getUrl(), DIR);
            if (url == null) {
                continue;
            }
            deleteOnRollback(url);
            WmsMaterialImage image = new WmsMaterialImage();
            image.setMaterialId(toMaterialId);
            image.setUrl(url);
            image.setFileName(source.getFileName());
            image.setFileSize(source.getFileSize());
            image.setSort(++sort);
            imageMapper.insert(image);
            if (main == null) {
                main = url;
            }
        }
        if (main != null) {
            syncMain(toMaterialId, main);
        }
    }

    private void requireMaterial(Long materialId) {
        WmsMaterial material = materialId == null ? null : materialMapper.selectById(materialId);
        if (material == null) {
            throw new BizException("wms.error.materialNotFound");
        }
    }

    private void syncMain(Long materialId, String url) {
        materialMapper.update(Wrappers.<WmsMaterial>lambdaUpdate()
                .set(WmsMaterial::getImageUrl, url)
                .eq(WmsMaterial::getId, materialId));
    }

    private static String fileName(String original) {
        if (original == null || original.isBlank()) {
            return null;
        }
        String name = Path.of(original.replace('\\', '/')).getFileName().toString();
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private void deleteOnRollback(String url) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    fileStorageService.deleteImage(url);
                }
            }
        });
    }

    private void deleteAfterCommit(List<String> urls) {
        List<String> files = urls.stream().filter(Objects::nonNull).toList();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            files.forEach(fileStorageService::deleteImage);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                files.forEach(fileStorageService::deleteImage);
            }
        });
    }
}
