package com.kedaxunfei.myinterviewer.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kedaxunfei.myinterviewer.common.BusinessException;
import com.kedaxunfei.myinterviewer.common.ErrorCodes;
import com.kedaxunfei.myinterviewer.domain.InterviewerStyle;
import com.kedaxunfei.myinterviewer.dto.InterviewerStyleResponse;
import com.kedaxunfei.myinterviewer.repository.InterviewerStyleMapper;

@Service
public class InterviewerStyleService {

    private final InterviewerStyleMapper interviewerStyleMapper;

    public InterviewerStyleService(InterviewerStyleMapper interviewerStyleMapper) {
        this.interviewerStyleMapper = interviewerStyleMapper;
    }

    public List<InterviewerStyleResponse> listEnabledStyles() {
        return interviewerStyleMapper.selectList(baseOrder()
                        .eq(InterviewerStyle::getEnabled, true))
                .stream()
                .map(InterviewerStyleResponse::from)
                .toList();
    }

    InterviewerStyle requireEnabledStyle(Long id) {
        InterviewerStyle style = interviewerStyleMapper.selectById(id);
        if (style == null || !Boolean.TRUE.equals(style.getEnabled())) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "面试官风格不存在或已停用");
        }
        return style;
    }

    InterviewerStyle requireStyle(Long id) {
        InterviewerStyle style = interviewerStyleMapper.selectById(id);
        if (style == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "面试官风格不存在");
        }
        return style;
    }

    Map<Long, InterviewerStyle> selectMapByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return interviewerStyleMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(InterviewerStyle::getId, s -> s));
    }

    private LambdaQueryWrapper<InterviewerStyle> baseOrder() {
        return new LambdaQueryWrapper<InterviewerStyle>()
                .orderByAsc(InterviewerStyle::getSortOrder)
                .orderByAsc(InterviewerStyle::getId);
    }
}
