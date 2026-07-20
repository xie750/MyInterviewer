package com.kedaxunfei.myinterviewer.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kedaxunfei.myinterviewer.common.BusinessException;
import com.kedaxunfei.myinterviewer.common.ErrorCodes;
import com.kedaxunfei.myinterviewer.domain.PostureEventType;
import com.kedaxunfei.myinterviewer.domain.PostureThresholdConfig;
import com.kedaxunfei.myinterviewer.dto.PostureThresholdRequest;
import com.kedaxunfei.myinterviewer.dto.PostureThresholdResponse;
import com.kedaxunfei.myinterviewer.repository.PostureThresholdConfigMapper;

@Service
public class PostureThresholdService {

    private final PostureThresholdConfigMapper postureThresholdConfigMapper;

    public PostureThresholdService(PostureThresholdConfigMapper postureThresholdConfigMapper) {
        this.postureThresholdConfigMapper = postureThresholdConfigMapper;
    }

    public List<PostureThresholdResponse> listAll() {
        return postureThresholdConfigMapper.selectList(baseOrder()).stream()
                .map(PostureThresholdResponse::from)
                .toList();
    }

    public List<PostureThresholdResponse> listEnabled() {
        return postureThresholdConfigMapper.selectList(baseOrder()
                        .eq(PostureThresholdConfig::getEnabled, true))
                .stream()
                .map(PostureThresholdResponse::from)
                .toList();
    }

    @Transactional
    public PostureThresholdResponse update(Long id, PostureThresholdRequest request) {
        PostureThresholdConfig config = requireConfig(id);
        ensureEventTypeAvailable(request.eventType(), id);
        fill(config, request);
        config.setUpdatedAt(LocalDateTime.now());
        postureThresholdConfigMapper.updateById(config);
        return PostureThresholdResponse.from(requireConfig(id));
    }

    private LambdaQueryWrapper<PostureThresholdConfig> baseOrder() {
        return new LambdaQueryWrapper<PostureThresholdConfig>()
                .orderByAsc(PostureThresholdConfig::getSortOrder)
                .orderByAsc(PostureThresholdConfig::getId);
    }

    private PostureThresholdConfig requireConfig(Long id) {
        PostureThresholdConfig config = postureThresholdConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "姿态阈值配置不存在");
        }
        return config;
    }

    private void ensureEventTypeAvailable(PostureEventType eventType, Long currentId) {
        LambdaQueryWrapper<PostureThresholdConfig> wrapper = new LambdaQueryWrapper<PostureThresholdConfig>()
                .eq(PostureThresholdConfig::getEventType, eventType);
        if (currentId != null) {
            wrapper.ne(PostureThresholdConfig::getId, currentId);
        }
        if (postureThresholdConfigMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "姿态事件类型配置已存在");
        }
    }

    private void fill(PostureThresholdConfig config, PostureThresholdRequest request) {
        config.setEventType(request.eventType());
        config.setDisplayName(normalize(request.displayName()));
        config.setDescription(normalizeNullable(request.description()));
        config.setWarningThreshold(request.warningThreshold());
        config.setCriticalThreshold(request.criticalThreshold());
        config.setEnabled(request.enabled() == null || request.enabled());
        config.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
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
