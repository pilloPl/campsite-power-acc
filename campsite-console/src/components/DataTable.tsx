import { Box } from '@mui/material';
import { DataGrid, type DataGridProps, type GridValidRowModel } from '@mui/x-data-grid';

const inTests = import.meta.env.MODE === 'test';

export type DataTableProps<R extends GridValidRowModel> = Omit<DataGridProps<R>, 'rows'> & {
  rows: readonly R[] | undefined;
  /** Height of the grid in pixels (the grid scrolls inside); `auto` grows with the rows. */
  height?: number | 'auto';
  testId?: string;
};

/**
 * MUI X Data Grid (MIT) with the defaults of the console: compact rows, client paging, row id `id`; a caller's `sx` is
 * added to the default styles.
 */
export function DataTable<R extends GridValidRowModel>({ rows, height = 460, testId, sx, ...props }: DataTableProps<R>) {
  return (
    <Box sx={{ height: height === 'auto' ? undefined : height, width: '100%' }} data-testid={testId}>
      <DataGrid<R>
        rows={rows ?? []}
        density="compact"
        disableRowSelectionOnClick
        disableVirtualization={inTests}
        autoHeight={height === 'auto'}
        pageSizeOptions={[10, 25, 50, 100]}
        initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        hideFooterSelectedRowCount
        {...props}
        sx={[
          { bgcolor: 'background.paper', '& .MuiDataGrid-columnHeaderTitle': { fontWeight: 600 } },
          ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
        ]}
      />
    </Box>
  );
}
