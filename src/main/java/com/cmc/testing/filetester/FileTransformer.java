package com.cmc.testing.filetester;

import com.google.common.base.Strings;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

@Component
public class FileTransformer {
    private static final String EXPORT_TYPE_DELIMITED = "Delimited";
    private static final List<String> EXPORT_TYPES = Arrays.asList("Fixed Length", EXPORT_TYPE_DELIMITED);

    /**
     * Transform an Excel file that specifies a file loader format sheet and data sheet into
     * the correct output file.
     *
     * @param bytes byte array containing Excel file in binary format
     * @return byte array of output text file
     * @throws FileTransformException
     * @throws IOException
     */
    public byte[] transform(final byte[] bytes) throws FileTransformException, IOException {
        Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        validateWorkbook(wb);

        return transformData(wb);
    }

    /**
     * Transforms the data from the Workbook.
     *
     * @param wb the Workbook
     * @return the output file as a byte[]
     * @throws FileTransformException when the data sheet is in an unexpected format
     */
    private byte[] transformData(Workbook wb) throws FileTransformException {
        ExportOptions exportOptions = getExportOptions(wb);

        Sheet dataSheet = wb.getSheetAt(1);
        validateDataSheet(dataSheet);

        // Map the fields to the data sheet column headings: key=field/column name, value=data sheet cell index
        Map<String, Integer> fieldMap = buildFieldMap(dataSheet);
        validateDataFields(exportOptions.getFields(), fieldMap);

        Iterator<Row> rowIt = dataSheet.rowIterator();
        rowIt.next(); // skip column heading

        StringBuilder outputFileBuilder = new StringBuilder();
        List<String> errors = new ArrayList<String>();
        DataFormatter formatter = new DataFormatter(true);
        while (rowIt.hasNext()) {
            Row dataRow = rowIt.next();

            Iterator<Field> fieldIterator = exportOptions.getFields().iterator();
            while(fieldIterator.hasNext()) {
                Field field = fieldIterator.next();
                try {
                    Cell cell = dataRow.getCell(fieldMap.get(field.getName()), Row.CREATE_NULL_AS_BLANK);
                    String value = formatter.formatCellValue(cell);

                    if (exportOptions.isDelimited()) {
                        outputFileBuilder.append(value);
                        if (fieldIterator.hasNext()) {
                            outputFileBuilder.append(exportOptions.getDelimiter());
                        }
                    } else {
                        outputFileBuilder.append(Strings.padEnd(value, field.getLength(), ' '));
                    }
                } catch (Exception e) {
                    errors.add(String.format("Data row %d, exception: %s", dataRow.getRowNum(), e.getMessage()));
                }
            }

            outputFileBuilder.append(System.lineSeparator());
        }

        if (!errors.isEmpty()) {
            throw new FileTransformException(errors);
        }

        return outputFileBuilder.toString().getBytes();
    }

    /**
     * Extract the ExportOptions from the Workbook.
     *
     * @param wb the Workbook
     * @return ExportOptions
     */
    private ExportOptions getExportOptions(Workbook wb) {
        Sheet formatSheet = wb.getSheetAt(0);
        ExportOptions exportOptions = validateFormatSheet(formatSheet);
        exportOptions.addFields(extractFields(formatSheet, exportOptions));
        return exportOptions;
    }

    /**
     * Extracts the format fields from the format Worksheet.
     *
     * @param formatSheet th format worksheet
     * @param exportOptions ExportOptions
     * @return list of file format fields
     * @throws FileTransformException when the sheet is in an unexpected format
     */
    private List<Field> extractFields(Sheet formatSheet, ExportOptions exportOptions) throws FileTransformException {
        int numCells = exportOptions.isDelimited() ? 1 : 2;
        Iterator<Row> rowIt = formatSheet.rowIterator();
        rowIt.next(); // skip option headers
        rowIt.next(); // skip option values
        rowIt.next(); // skip empty row
        rowIt.next(); // skip column headers

        List<Field> fields = new ArrayList<Field>();
        List<String> errors = new ArrayList<String>();
        while(rowIt.hasNext()) {
            Row row = rowIt.next();

            if (row.getLastCellNum() < numCells) {
                errors.add(String.format("Format sheet, row %d: Should have 2 cells.", row.getRowNum()));
            } else {
                String fieldName = row.getCell(0).getStringCellValue();

                if (exportOptions.isDelimited()) {
                    fields.add(new Field(fieldName));
                } else {
                    Cell lengthCell = row.getCell(1);
                    if (lengthCell.getCellType() != Cell.CELL_TYPE_NUMERIC) {
                        errors.add(String.format("Format sheet, row %d: Length cell is not numeric.", row.getRowNum()));
                    } else {
                        fields.add(new Field(fieldName, (int) lengthCell.getNumericCellValue()));
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new FileTransformException(errors);
        }

        return fields;
    }

    /**
     * From the data Sheet, creates a map with key=column header name, value=column index
     *
     * @param dataSheet the data sheet
     * @return a map
     */
    private Map<String, Integer> buildFieldMap(Sheet dataSheet) {
        Row labels = dataSheet.getRow(0);
        Iterator<Cell> cellIterator = labels.cellIterator();
        Map<String,Integer> fieldMap = new HashMap<String,Integer>();
        while(cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            fieldMap.put(cell.getStringCellValue(), cell.getColumnIndex());
        }
        return fieldMap;
    }

    /**
     * Validates that the Workbook generally matches the expected structure.
     *
     * @param wb the Workbook
     * @throws FileTransformException
     */
    private void validateWorkbook(Workbook wb) throws FileTransformException {
        List<String> errors = new ArrayList<String>();
        if (wb == null) {
            errors.add("Workbook was null");
        } else {
            if (wb.getNumberOfSheets() != 2) {
                errors.add("There should be exactly 2 worksheets.");
            }
        }
        if (!errors.isEmpty()) {
            throw new FileTransformException(errors);
        }
    }

    /**
     * Validates that the Format Sheet is generally formatted as expected.
     *
     * @param formatSheet the format Sheet
     * @throws FileTransformException
     */
    private ExportOptions validateFormatSheet(Sheet formatSheet) throws FileTransformException {
        if (formatSheet.getLastRowNum() < 5) {
            throw new FileTransformException(Collections.singletonList("Format sheet should have at least 5 rows."));
        }

        String exportType = formatSheet.getRow(1).getCell(0, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
        String delimiter = formatSheet.getRow(1).getCell(1, Row.CREATE_NULL_AS_BLANK).getStringCellValue();
        boolean delimited = exportType.equals(EXPORT_TYPE_DELIMITED);

        if (!EXPORT_TYPES.contains(exportType)) {
            throw new FileTransformException(Collections.singletonList("Export Type must be in list: " + EXPORT_TYPES));
        } else if (delimited && StringUtils.isEmpty(delimiter)) {
            throw new FileTransformException(Collections.singletonList("Must choose delimiter for Delimited export type."));
        }

        return new ExportOptions(delimited, delimiter);
    }

    /**
     * Validates that the Data Sheet is generally formatted as expected.
     *
     * @param dataSheet the data Sheet
     * @throws FileTransformException
     */
    private void validateDataSheet(Sheet dataSheet) throws FileTransformException {
        if (dataSheet.getLastRowNum() < 2) {
            throw new FileTransformException(Collections.singletonList("Data sheet should have at least 2 rows."));
        }
    }

    /**
     * validates that the column headings on the Data Sheet and the field names on the Format Sheet match.
     *
     * @param fields fields in the Format Sheet
     * @param fieldMap map of fields from the Data Sheet
     * @throws FileTransformException
     */
    private void validateDataFields(List<Field> fields, Map<String, Integer> fieldMap) throws FileTransformException {
        List<String> errors = new ArrayList<String>();
        if (fields.size() != fieldMap.size()) {
            errors.add(String.format(
                    "There should be 1 data column for every format field: %d data columns, %d format fields.",
                    fieldMap.size(),
                    fields.size()));
        }

        for (Field field : fields) {
            if (!fieldMap.containsKey(field.getName())) {
                errors.add(String.format("There is no data column for field '%s'", field.getName()));
            }
        }

        if (!errors.isEmpty()) {
            throw new FileTransformException(errors);
        }
    }
}
