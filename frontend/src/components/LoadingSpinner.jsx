export default function LoadingSpinner({ label = 'Loading…' }) {
  return (
    <div className="flex flex-col justify-center items-center gap-3 py-20">
      <div className="relative w-12 h-12">
        <div className="absolute inset-0 rounded-full border-4 border-slate-200" />
        <div className="absolute inset-0 rounded-full border-4 border-blue-500 border-t-transparent animate-spin" />
      </div>
      <p className="text-sm text-slate-400 font-medium">{label}</p>
    </div>
  );
}
