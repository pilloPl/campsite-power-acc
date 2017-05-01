/** Client-side export of report lines: CSV and an Excel workbook (SpreadsheetML 2003 XML, opens in Excel). */

export interface ExportColumn<R> {
  header: string;
  value: (row: R) => string | number | null | undefined;
}

function csvCell(value: string | number | null | undefined): string {
  if (value === null || value === undefined) return '';
  const text = String(value);
  return /[",;\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

export function toCsv<R>(rows: readonly R[], columns: ExportColumn<R>[]): string {
  const lines = [columns.map((column) => csvCell(column.header)).join(',')];
  rows.forEach((row) => lines.push(columns.map((column) => csvCell(column.value(row))).join(',')));
  return `${lines.join('\r\n')}\r\n`;
}

function xmlText(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function xmlCell(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') return '<Cell/>';
  if (typeof value === 'number' && Number.isFinite(value)) return `<Cell><Data ss:Type="Number">${value}</Data></Cell>`;
  return `<Cell><Data ss:Type="String">${xmlText(String(value))}</Data></Cell>`;
}

/** One worksheet with a bold header row. */
export function toSpreadsheetXml<R>(rows: readonly R[], columns: ExportColumn<R>[], sheetName: string): string {
  const header = `<Row>${columns.map((column) => `<Cell ss:StyleID="h"><Data ss:Type="String">${xmlText(column.header)}</Data></Cell>`).join('')}</Row>`;
  const body = rows.map((row) => `<Row>${columns.map((column) => xmlCell(column.value(row))).join('')}</Row>`).join('');
  return '<?xml version="1.0" encoding="UTF-8"?>\n<?mso-application progid="Excel.Sheet"?>\n'
    + '<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">'
    + '<Styles><Style ss:ID="h"><Font ss:Bold="1"/></Style></Styles>'
    + `<Worksheet ss:Name="${xmlText(sheetName.substring(0, 31))}"><Table>${header}${body}</Table></Worksheet></Workbook>`;
}

export function downloadFile(content: string, fileName: string, mimeType: string) {
  const url = URL.createObjectURL(new Blob([content], { type: mimeType }));
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export function exportCsv<R>(rows: readonly R[], columns: ExportColumn<R>[], baseName: string) {
  downloadFile(toCsv(rows, columns), `${baseName}.csv`, 'text/csv;charset=utf-8');
}

export function exportSpreadsheet<R>(rows: readonly R[], columns: ExportColumn<R>[], baseName: string) {
  downloadFile(toSpreadsheetXml(rows, columns, baseName), `${baseName}.xls`, 'application/vnd.ms-excel');
}
