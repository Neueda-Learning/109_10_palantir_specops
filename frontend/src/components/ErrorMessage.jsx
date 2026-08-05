export default function ErrorMessage({ message }) {
  return (
    <div className="flex items-start gap-3 bg-red-50 border border-red-200 text-red-700 rounded-xl p-4 text-sm">
      <span className="text-lg leading-none mt-0.5">&#9888;</span>
      <span>{message || 'An unexpected error occurred.'}</span>
    </div>
  );
}
