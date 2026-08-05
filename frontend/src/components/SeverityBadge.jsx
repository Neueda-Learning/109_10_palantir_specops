const styles = {
  HIGH:   { dot: 'bg-red-500',    pill: 'bg-red-50 text-red-700 ring-1 ring-red-200/80' },
  MEDIUM: { dot: 'bg-amber-500',  pill: 'bg-amber-50 text-amber-700 ring-1 ring-amber-200/80' },
  LOW:    { dot: 'bg-blue-500',   pill: 'bg-blue-50 text-blue-700 ring-1 ring-blue-200/80' },
};

export default function SeverityBadge({ severity }) {
  const s = styles[severity] ?? { dot: 'bg-gray-400', pill: 'bg-gray-50 text-gray-600 ring-1 ring-gray-200' };
  return (
    <span className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full whitespace-nowrap ${s.pill}`}>
      <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${s.dot}`} />
      {severity}
    </span>
  );
}
