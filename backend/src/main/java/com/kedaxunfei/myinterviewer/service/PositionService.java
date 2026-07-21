package com.kedaxunfei.myinterviewer.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kedaxunfei.myinterviewer.common.BusinessException;
import com.kedaxunfei.myinterviewer.common.ErrorCodes;
import com.kedaxunfei.myinterviewer.domain.JobPosition;
import com.kedaxunfei.myinterviewer.dto.PositionEnabledRequest;
import com.kedaxunfei.myinterviewer.dto.PositionRequest;
import com.kedaxunfei.myinterviewer.dto.PositionResponse;
import com.kedaxunfei.myinterviewer.repository.JobPositionMapper;

@Service
public class PositionService {

    private final JobPositionMapper jobPositionMapper;

    public PositionService(JobPositionMapper jobPositionMapper) {
        this.jobPositionMapper = jobPositionMapper;
    }

    public List<PositionResponse> listEnabledPositions() {
        return jobPositionMapper.selectList(baseOrder()
                        .eq(JobPosition::getEnabled, true))
                .stream()
                .map(PositionResponse::from)
                .toList();
    }

    public List<PositionResponse> listAllPositions() {
        return jobPositionMapper.selectList(baseOrder())
                .stream()
                .map(PositionResponse::from)
                .toList();
    }

    @Transactional
    public PositionResponse createPosition(PositionRequest request) {
        ensureNameAvailable(request.name(), null);

        LocalDateTime now = LocalDateTime.now();
        JobPosition position = new JobPosition();
        fillPosition(position, request);
        position.setEnabled(request.enabled() == null || request.enabled());
        position.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        position.setCreatedAt(now);
        position.setUpdatedAt(now);
        jobPositionMapper.insert(position);
        return PositionResponse.from(position);
    }

    @Transactional
    public PositionResponse updatePosition(Long id, PositionRequest request) {
        JobPosition position = requirePosition(id);
        ensureNameAvailable(request.name(), id);

        fillPosition(position, request);
        position.setEnabled(request.enabled() == null || request.enabled());
        position.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        position.setUpdatedAt(LocalDateTime.now());
        jobPositionMapper.updateById(position);
        return PositionResponse.from(requirePosition(id));
    }

    @Transactional
    public PositionResponse updateEnabled(Long id, PositionEnabledRequest request) {
        JobPosition position = requirePosition(id);
        position.setEnabled(request.enabled());
        position.setUpdatedAt(LocalDateTime.now());
        jobPositionMapper.updateById(position);
        return PositionResponse.from(requirePosition(id));
    }

    private LambdaQueryWrapper<JobPosition> baseOrder() {
        return new LambdaQueryWrapper<JobPosition>()
                .orderByAsc(JobPosition::getSortOrder)
                .orderByAsc(JobPosition::getId);
    }

    JobPosition requireEnabledPosition(Long id) {
        JobPosition position = requirePosition(id);
        if (!Boolean.TRUE.equals(position.getEnabled())) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "岗位不存在或已停用");
        }
        return position;
    }

    JobPosition requirePosition(Long id) {
        JobPosition position = jobPositionMapper.selectById(id);
        if (position == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "岗位不存在");
        }
        return position;
    }

    Map<Long, JobPosition> selectMapByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return jobPositionMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(JobPosition::getId, p -> p));
    }

    private void ensureNameAvailable(String name, Long currentId) {
        LambdaQueryWrapper<JobPosition> wrapper = new LambdaQueryWrapper<JobPosition>()
                .eq(JobPosition::getName, normalize(name));
        if (currentId != null) {
            wrapper.ne(JobPosition::getId, currentId);
        }
        if (jobPositionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "岗位名称已存在");
        }
    }

    private void fillPosition(JobPosition position, PositionRequest request) {
        position.setName(normalize(request.name()));
        position.setDescription(normalizeNullable(request.description()));
        position.setTechStack(normalizeNullable(request.techStack()));
        position.setDifficulty(normalizeNullable(request.difficulty()));
        position.setPromptTemplate(normalizeNullable(request.promptTemplate()));
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
