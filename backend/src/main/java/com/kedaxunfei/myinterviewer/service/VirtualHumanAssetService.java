package com.kedaxunfei.myinterviewer.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kedaxunfei.myinterviewer.common.BusinessException;
import com.kedaxunfei.myinterviewer.common.ErrorCodes;
import com.kedaxunfei.myinterviewer.domain.VirtualHumanAsset;
import com.kedaxunfei.myinterviewer.dto.VirtualHumanAssetRequest;
import com.kedaxunfei.myinterviewer.dto.VirtualHumanAssetResponse;
import com.kedaxunfei.myinterviewer.repository.VirtualHumanAssetMapper;

@Service
public class VirtualHumanAssetService {

    private final VirtualHumanAssetMapper virtualHumanAssetMapper;

    public VirtualHumanAssetService(VirtualHumanAssetMapper virtualHumanAssetMapper) {
        this.virtualHumanAssetMapper = virtualHumanAssetMapper;
    }

    public List<VirtualHumanAssetResponse> listAssets(String keyword, Boolean enabled) {
        return virtualHumanAssetMapper.selectList(baseOrder()).stream()
                .filter(asset -> enabled == null || Boolean.TRUE.equals(asset.getEnabled()) == enabled)
                .filter(asset -> matchesKeyword(asset, keyword))
                .map(VirtualHumanAssetResponse::from)
                .toList();
    }

    @Transactional
    public VirtualHumanAssetResponse createAsset(VirtualHumanAssetRequest request) {
        ensureKeyAvailable(request.assetKey(), null);
        LocalDateTime now = LocalDateTime.now();
        VirtualHumanAsset asset = new VirtualHumanAsset();
        fill(asset, request);
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);
        virtualHumanAssetMapper.insert(asset);
        return VirtualHumanAssetResponse.from(asset);
    }

    @Transactional
    public VirtualHumanAssetResponse updateAsset(Long id, VirtualHumanAssetRequest request) {
        VirtualHumanAsset asset = requireAsset(id);
        ensureKeyAvailable(request.assetKey(), id);
        fill(asset, request);
        asset.setUpdatedAt(LocalDateTime.now());
        virtualHumanAssetMapper.updateById(asset);
        return VirtualHumanAssetResponse.from(requireAsset(id));
    }

    private LambdaQueryWrapper<VirtualHumanAsset> baseOrder() {
        return new LambdaQueryWrapper<VirtualHumanAsset>()
                .orderByAsc(VirtualHumanAsset::getSortOrder)
                .orderByAsc(VirtualHumanAsset::getId);
    }

    private VirtualHumanAsset requireAsset(Long id) {
        VirtualHumanAsset asset = virtualHumanAssetMapper.selectById(id);
        if (asset == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "虚拟人素材不存在");
        }
        return asset;
    }

    private void ensureKeyAvailable(String key, Long currentId) {
        LambdaQueryWrapper<VirtualHumanAsset> wrapper = new LambdaQueryWrapper<VirtualHumanAsset>()
                .eq(VirtualHumanAsset::getAssetKey, normalize(key));
        if (currentId != null) {
            wrapper.ne(VirtualHumanAsset::getId, currentId);
        }
        if (virtualHumanAssetMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "虚拟人资源 key 已存在");
        }
    }

    private void fill(VirtualHumanAsset asset, VirtualHumanAssetRequest request) {
        asset.setAssetKey(normalize(request.assetKey()));
        asset.setName(normalize(request.name()));
        asset.setDescription(normalizeNullable(request.description()));
        asset.setImageUrl(normalizeNullable(request.imageUrl()));
        asset.setAccentColor(normalizeNullable(request.accentColor()));
        asset.setBadge(normalizeNullable(request.badge()));
        asset.setEnabled(request.enabled() == null || request.enabled());
        asset.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private boolean matchesKeyword(VirtualHumanAsset asset, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        return contains(asset.getAssetKey(), normalized)
                || contains(asset.getName(), normalized)
                || contains(asset.getDescription(), normalized)
                || contains(asset.getBadge(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
