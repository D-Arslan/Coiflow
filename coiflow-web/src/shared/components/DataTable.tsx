export interface Column<T> {
  header: string;
  accessor: keyof T | ((row: T) => string | number | boolean | null | undefined);
  className?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  actions?: (row: T) => React.ReactNode;
  keyExtractor: (row: T) => string;
  isLoading?: boolean;
  emptyMessage?: string;
}

export function DataTable<T>({
  columns,
  data,
  actions,
  keyExtractor,
  isLoading = false,
  emptyMessage = 'Aucun resultat',
}: DataTableProps<T>) {
  if (isLoading) {
    return (
      <div className="flex justify-center py-12">
        <div className="h-8 w-8 animate-spin rounded-full border-b-2 border-amber-500" />
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-stone-200 bg-white shadow-sm">
      <table className="min-w-full divide-y divide-stone-200">
        <thead className="bg-stone-50">
          <tr>
            {columns.map((col) => (
              <th
                key={String(col.header)}
                className={`px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-stone-500 ${col.className ?? ''}`}
              >
                {col.header}
              </th>
            ))}
            {actions && (
              <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-stone-500">
                Actions
              </th>
            )}
          </tr>
        </thead>
        <tbody className="divide-y divide-stone-200">
          {data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length + (actions ? 1 : 0)}
                className="px-6 py-8 text-center text-sm text-stone-500"
              >
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((row) => (
              <tr key={keyExtractor(row)} className="hover:bg-stone-50 transition-colors">
                {columns.map((col) => {
                  const value =
                    typeof col.accessor === 'function'
                      ? col.accessor(row)
                      : row[col.accessor];
                  return (
                    <td
                      key={String(col.header)}
                      className={`whitespace-nowrap px-6 py-4 text-sm text-stone-800 ${col.className ?? ''}`}
                    >
                      {value == null ? '\u2014' : String(value)}
                    </td>
                  );
                })}
                {actions && (
                  <td className="whitespace-nowrap px-6 py-4 text-right text-sm">
                    {actions(row)}
                  </td>
                )}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
