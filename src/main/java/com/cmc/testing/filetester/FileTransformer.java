package com.cmc.testing.filetester;

import com.google.common.base.Strings;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

@Component
public class FileTransformer {

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
        return transformData(wb, extractFields(wb));
    }

    /**
     * Extracts the format fields from the first Sheet in the specified Workbook.
     *
     * @param wb th Workbook
     * @return list of file format fields
     * @throws FileTransformException when the sheet is in an unexpected format
     */
    private List<Field> extractFields(Workbook wb) throws FileTransformException {
        Sheet formatSheet = wb.getSheetAt(0);
        validateFormatSheet(formatSheet);

        Iterator<Row> rowIt = formatSheet.rowIterator();
        rowIt.next(); // skip column headers

        List<Field> fields = new ArrayList<Field>();
        List<String> errors = new ArrayList<String>();
        while(rowIt.hasNext()) {
            Row row = rowIt.next();

            if (row.getLastCellNum() < 2) {
                errors.add(String.format("Format sheet, row %d: Should have 2 cells.", row.getRowNum()));
            } else {
                String fieldName = row.getCell(0).getStringCellValue();

                Cell lengthCell = row.getCell(1);
                if (lengthCell.getCellType() != Cell.CELL_TYPE_NUMERIC) {
                    errors.add(String.format("Format sheet, row %d: Length cell is not numeric.", row.getRowNum()));
                } else {
                    fields.add(new Field(fieldName, (int) lengthCell.getNumericCellValue()));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new FileTransformException(errors);
        }

        return fields;
    }

    /**
     * Transforms the data from the second Sheet in the specified Workbook using the specified format fields.
     *
     * @param wb the workbook
     * @param fields the fields
     * @return the output file as a byte[]
     * @throws FileTransformException when the data sheet is in an unexpected format
     */
    private byte[] transformData(Workbook wb, List<Field> fields) throws FileTransformException {
        Sheet dataSheet = wb.getSheetAt(1);
        validateDataSheet(dataSheet);

        // Map the fields to the data sheet column headings: key=field/column name, value=data sheet cell index
        Map<String, Integer> fieldMap = buildFieldMap(dataSheet);
        validateDataFields(fields, fieldMap);

        Iterator<Row> rowIt = dataSheet.rowIterator();
        rowIt.next(); // skip column heading

        StringBuilder outputFileBuilder = new StringBuilder();
        List<String> errors = new ArrayList<String>();
        DataFormatter formatter = new DataFormatter(true);
        while (rowIt.hasNext()) {
            Row dataRow = rowIt.next();

            for (Field field : fields) {
                try {
                    Cell cell = dataRow.getCell(fieldMap.get(field.getName()), Row.CREATE_NULL_AS_BLANK);

                    outputFileBuilder.append(Strings.padEnd(formatter.formatCellValue(cell), field.getLength(), ' '));
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
    private void validateFormatSheet(Sheet formatSheet) throws FileTransformException {
        if (formatSheet.getLastRowNum() < 2) {
            throw new FileTransformException(Collections.singletonList("Format sheet should have at least 2 rows."));
        }
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

        for(Field field : fields) {
            if (!fieldMap.containsKey(field.getName())) {
                errors.add(String.format("There is no data column for field '%s'", field.getName()));
            }
        }

        if (!errors.isEmpty()) {
            throw new FileTransformException(errors);
        }
    }
}
