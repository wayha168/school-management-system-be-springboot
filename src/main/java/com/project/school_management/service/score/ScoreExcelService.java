package com.project.school_management.service.score;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.school_management.dto.score.ScoreRequest;
import com.project.school_management.dto.score.ScoreResponse;
import com.project.school_management.exception.ExceptionNotFound;

@Service
public class ScoreExcelService {

    public byte[] export(List<ScoreResponse> rows, Function<UUID, String> emailLookup) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Scores");
            Row header = sheet.createRow(0);
            String[] cols = {
                    "Student Email", "Student Name", "Class", "Generation", "Subject", "Term", "Score", "Max Score",
                    "Remark"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            int r = 1;
            for (ScoreResponse row : rows) {
                Row excelRow = sheet.createRow(r++);
                excelRow.createCell(0).setCellValue(nullToEmpty(emailLookup.apply(row.getStudentUuid())));
                excelRow.createCell(1).setCellValue(nullToEmpty(row.getStudentName()));
                excelRow.createCell(2).setCellValue(nullToEmpty(row.getClassName()));
                excelRow.createCell(3).setCellValue(row.getGeneration() == null ? "" : "G" + row.getGeneration());
                excelRow.createCell(4).setCellValue(nullToEmpty(row.getSubject()));
                excelRow.createCell(5).setCellValue(nullToEmpty(row.getTerm()));
                excelRow.createCell(6).setCellValue(row.getScore() == null ? 0 : row.getScore().doubleValue());
                excelRow.createCell(7).setCellValue(row.getMaxScore() == null ? 100 : row.getMaxScore().doubleValue());
                excelRow.createCell(8).setCellValue(nullToEmpty(row.getRemark()));
            }
            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to export Excel: " + ex.getMessage(), ex);
        }
    }

    public int importRows(MultipartFile file, UUID classUuid, BiConsumer<ScoreRequest, String> rowHandler) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }
        int imported = 0;
        DataFormatter formatter = new DataFormatter();
        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String email = formatter.formatCellValue(row.getCell(0)).trim();
                String subject = formatter.formatCellValue(row.getCell(4)).trim();
                String term = formatter.formatCellValue(row.getCell(5)).trim();
                String scoreText = formatter.formatCellValue(row.getCell(6)).trim();
                String maxText = formatter.formatCellValue(row.getCell(7)).trim();
                String remark = formatter.formatCellValue(row.getCell(8)).trim();
                if (email.isBlank() || subject.isBlank() || scoreText.isBlank()) {
                    continue;
                }
                ScoreRequest request = new ScoreRequest();
                request.setClassUuid(classUuid);
                request.setSubject(subject);
                request.setTerm(term.isBlank() ? "Term 1" : term);
                request.setScore(new BigDecimal(scoreText));
                request.setMaxScore(maxText.isBlank() ? BigDecimal.valueOf(100) : new BigDecimal(maxText));
                request.setRemark(remark.isBlank() ? null : remark);
                rowHandler.accept(request, email);
                imported++;
            }
        } catch (ExceptionNotFound | IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to import Excel: " + ex.getMessage(), ex);
        }
        return imported;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
