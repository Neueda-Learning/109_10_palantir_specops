const styles = {
  OPEN:          { dot: 'bg-red-500 animate-pulse',   pill: 'bg-red-50 text-red-700 ring-1 ring-red-200/80' },
  ACKNOWLEDGED:  { dot: 'bg-amber-500',               pill: 'bg-amber-50 text-amber-700 ring-1 ring-amber-200/80' },
  INVESTIGATING: { dot: 'bg-orange-500',              pill: 'bg-orange-50 text-orange-700 ring-1 ring-orange-200/80' },
  CLOSED:        { dot: 'bg-emerald-500',             pill: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200/80' },
  DISMISSED:     { dot: 'bg-slate-400',               pill: 'bg-slate-50 text-slate-500 ring-1 ring-slate-200/80' },
};

export default function StatusBadge({ status }) {
  const s = styles[status] ?? { dot: 'bg-gray-400', pill: 'bg-gray-50 text-gray-600 ring-1 ring-gray-200' };
  return (
    <span className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full whitespace-nowrap ${s.pill}`}>
      <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${s.dot}`} />
      {status}
    </span>
  );
}
