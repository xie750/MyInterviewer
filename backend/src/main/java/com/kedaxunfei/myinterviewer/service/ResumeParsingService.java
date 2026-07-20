package com.kedaxunfei.myinterviewer.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kedaxunfei.myinterviewer.common.BusinessException;
import com.kedaxunfei.myinterviewer.common.ErrorCodes;
import com.kedaxunfei.myinterviewer.dto.ResumeParseResponse;

@Service
public class ResumeParsingService {

    private static final long MAX_BYTES = 10 * 1024 * 1024;
    private static final int MAX_TEXT_CHARS = 12000;
    private static final Pattern BLANKS = Pattern.compile("\\s+");
    private static final List<String> SKILL_KEYWORDS = List.of(
            "Java", "Spring Boot", "Spring Cloud", "MySQL", "Redis", "MQ", "Kafka", "RabbitMQ",
            "Docker", "Kubernetes", "Vue", "React", "TypeScript", "JavaScript", "Vite",
            "Node.js", "Python", "Go", "Linux", "Nginx", "MyBatis", "JUnit", "Git"
    );

    public ResumeParseResponse parse(MultipartFile file) {
        validateFile(file);
        String fileName = safeFileName(file.getOriginalFilename());
        String text = extractText(file, extensionOf(fileName));
        String normalized = normalizeText(text);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "简历内容为空或无法提取有效文本");
        }
        if (normalized.length() > MAX_TEXT_CHARS) {
            normalized = normalized.substring(0, MAX_TEXT_CHARS);
        }

        List<String> skills = extractSkills(normalized);
        List<String> projects = extractProjects(normalized);
        List<String> warnings = buildWarnings(normalized, skills, projects);
        String summary = buildSummary(normalized, skills, projects);
        return new ResumeParseResponse(fileName, summary, skills, projects, warnings, normalized.length());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "请上传非空简历文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "简历文件不能超过 10MB");
        }
        String fileName = safeFileName(file.getOriginalFilename());
        String extension = extensionOf(fileName);
        if (!List.of("txt", "md", "pdf", "docx").contains(extension)) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "仅支持 txt、md、pdf、docx 格式简历");
        }
    }

    private String extractText(MultipartFile file, String extension) {
        try {
            byte[] bytes = file.getBytes();
            return switch (extension) {
                case "txt", "md" -> new String(bytes, StandardCharsets.UTF_8);
                case "pdf" -> extractPdf(bytes);
                case "docx" -> extractDocx(bytes);
                default -> throw new BusinessException(ErrorCodes.BAD_REQUEST, "不支持的简历格式");
            };
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "简历读取失败，请检查文件内容");
        }
    }

    private String extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .reduce("", (left, right) -> left + "\n" + right);
        }
    }

    private String normalizeText(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
                .replace('\u00A0', ' ')
                .replace("\r", "\n");
        return BLANKS.matcher(normalized).replaceAll(" ").trim();
    }

    private List<String> extractSkills(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        Set<String> hits = new LinkedHashSet<>();
        for (String skill : SKILL_KEYWORDS) {
            if (lower.contains(skill.toLowerCase(Locale.ROOT))) {
                hits.add(skill);
            }
        }
        return hits.stream().limit(12).toList();
    }

    private List<String> extractProjects(String text) {
        String[] sentences = text.split("[。！？!?；;\\n]");
        List<String> projects = new ArrayList<>();
        for (String sentence : sentences) {
            String item = sentence.trim();
            if (item.length() < 12) {
                continue;
            }
            if (containsAny(item, "项目", "系统", "平台", "服务", "负责", "参与", "主导", "设计", "开发", "优化")) {
                projects.add(limit(item, 110));
            }
            if (projects.size() >= 5) {
                break;
            }
        }
        return projects;
    }

    private List<String> buildWarnings(String text, List<String> skills, List<String> projects) {
        List<String> warnings = new ArrayList<>();
        if (skills.isEmpty()) {
            warnings.add("未提取到明确技能关键词，建议在面试中主动补充核心技术栈。");
        }
        if (projects.isEmpty()) {
            warnings.add("未提取到清晰项目经历，建议按 STAR 结构补充项目背景和结果。");
        }
        if (!containsAny(text, "指标", "提升", "降低", "增长", "%", "ms", "qps", "并发")) {
            warnings.add("简历中量化结果较少，建议准备性能、质量或业务指标。");
        }
        return warnings.stream().limit(4).toList();
    }

    private String buildSummary(String text, List<String> skills, List<String> projects) {
        String prefix = firstReadableSegment(text);
        String skillText = skills.isEmpty() ? "暂未提取到明确技能关键词" : "技能关键词：" + String.join("、", skills);
        String projectText = projects.isEmpty() ? "项目经历待面试中补充" : "代表经历：" + projects.get(0);
        return limit(prefix + "。" + skillText + "。" + projectText + "。", 500);
    }

    private String firstReadableSegment(String text) {
        String[] parts = text.split("[。！？!?；;]");
        for (String part : parts) {
            String item = part.trim();
            if (item.length() >= 20) {
                return limit(item, 160);
            }
        }
        return limit(text, 160);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String extensionOf(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "resume.txt";
        }
        return fileName.replace("\\", "/").substring(fileName.replace("\\", "/").lastIndexOf('/') + 1);
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
