import { toCsv, toSpreadsheetXml, type ExportColumn } from './exportTable';

interface Line { period: string; kwh?: number }
const columns: ExportColumn<Line>[] = [
  { header: 'Period', value: (row) => row.period },
  { header: 'kWh', value: (row) => row.kwh },
];

describe('export of report lines', () => {
  it('writes CSV with a header and quotes cells with separators', () => {
    expect(toCsv([{ period: '2026_09', kwh: 12.5 }, { period: 'A, "B"' }], columns))
      .toBe('Period,kWh\r\n2026_09,12.5\r\n"A, ""B""",\r\n');
  });

  it('writes a worksheet with number and text cells and escapes XML', () => {
    const xml = toSpreadsheetXml([{ period: '<Q3>', kwh: 3 }], columns, 'Quarterly consumption');
    expect(xml).toContain('<Worksheet ss:Name="Quarterly consumption">');
    expect(xml).toContain('<Data ss:Type="String">&lt;Q3&gt;</Data>');
    expect(xml).toContain('<Data ss:Type="Number">3</Data>');
  });
});
