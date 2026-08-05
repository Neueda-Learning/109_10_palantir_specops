import { useEffect, useState } from 'react';
import { getTransactions, generateTransactions, createTransaction } from '../services/transactionService';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';

const TYPES = ['DEBIT', 'CREDIT'];
const STATUSES = ['COMPLETED', 'PENDING', 'FAILED'];
const emptyTx = { accountId: '', payeeId: '', amount: '', type: 'DEBIT', status: 'COMPLETED', description: '' };

function CreateTransactionModal({ onClose, onSaved }) {
  const [form, setForm] = useState(emptyTx);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!form.accountId.trim() || !form.payeeId.trim() || !form.amount) {
      setError('Account ID, Payee ID and Amount are required.');
      return;
    }
    setSaving(true);
    setError(null);
    createTransaction({ ...form, amount: Number(form.amount) })
      .then(() => onSaved())
      .catch(e => setError(e.response?.data?.message || e.message))
      .finally(() => setSaving(false));
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-gray-800">Create Transaction</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl leading-none">&times;</button>
        </div>

        {error && <ErrorMessage message={error} />}

        <form onSubmit={handleSubmit} className="space-y-3">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-gray-600">Account ID *</label>
              <input
                required value={form.accountId}
                onChange={e => set('accountId', e.target.value)}
                placeholder="e.g. ACC-001"
                className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-gray-600">Payee ID *</label>
              <input
                required value={form.payeeId}
                onChange={e => set('payeeId', e.target.value)}
                placeholder="e.g. PAYEE-ABC"
                className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-gray-600">Amount ($) *</label>
            <input
              required type="number" min="0.01" step="0.01"
              value={form.amount}
              onChange={e => set('amount', e.target.value)}
              placeholder="0.00"
              className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-gray-600">Type</label>
              <select value={form.type} onChange={e => set('type', e.target.value)}
                className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                {TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-gray-600">Status</label>
              <select value={form.status} onChange={e => set('status', e.target.value)}
                className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-gray-600">Description</label>
            <input
              value={form.description}
              onChange={e => set('description', e.target.value)}
              placeholder="Optional note"
              className="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-xs text-blue-700">
            <strong>Note:</strong> Submitting will automatically evaluate all active monitoring rules and generate alerts if thresholds are breached.
          </div>

          <div className="flex justify-end gap-2 pt-1">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm rounded-lg border border-gray-300 hover:bg-gray-50 transition-colors">
              Cancel
            </button>
            <button type="submit" disabled={saving}
              className="px-4 py-2 text-sm rounded-lg bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50 transition-colors">
              {saving ? 'Submitting…' : 'Submit Transaction'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function Transactions() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [generating, setGenerating] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [successMsg, setSuccessMsg] = useState(null);

  // Filter state
  const [search, setSearch] = useState('');
  const [accountId, setAccountId] = useState('');
  const [minAmount, setMinAmount] = useState('');
  const [maxAmount, setMaxAmount] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [sortField, setSortField] = useState('timestamp');
  const [sortDir, setSortDir] = useState('desc');

  const load = (params = {}) => {
    setLoading(true);
    getTransactions(params)
      .then((r) => setTransactions(r.data))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const showSuccess = (msg) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(null), 4000);
  };

  const handleSearch = (e) => {
    e.preventDefault();
    const params = {};
    if (search) params.search = search;
    if (accountId) params.accountId = accountId;
    if (minAmount) params.minAmount = minAmount;
    if (maxAmount) params.maxAmount = maxAmount;
    if (from) params.from = from;
    if (to) params.to = to;
    load(params);
  };

  const handleGenerate = () => {
    setGenerating(true);
    generateTransactions(10)
      .then(() => { load(); showSuccess('10 random transactions generated.'); })
      .catch((e) => setError(e.message))
      .finally(() => setGenerating(false));
  };

  const handleCreated = () => {
    setShowCreate(false);
    load();
    showSuccess('Transaction submitted and rules evaluated.');
  };

  const toggleSort = (field) => {
    if (sortField === field) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    else { setSortField(field); setSortDir('asc'); }
  };

  const sorted = [...transactions].sort((a, b) => {
    const av = a[sortField], bv = b[sortField];
    if (av == null) return 1;
    if (bv == null) return -1;
    const cmp = av < bv ? -1 : av > bv ? 1 : 0;
    return sortDir === 'asc' ? cmp : -cmp;
  });

  const SortIcon = ({ field }) => (
    <span className="ml-1 text-gray-400 text-xs">
      {sortField === field ? (sortDir === 'asc' ? '▲' : '▼') : '⇅'}
    </span>
  );

  return (
    <div className="p-4 sm:p-6 space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Transactions</h1>
          <p className="text-sm text-slate-400 mt-0.5">Search, create and monitor transactions</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setShowCreate(true)}
            className="flex-1 sm:flex-none bg-blue-600 hover:bg-blue-700 text-white text-sm px-5 py-2.5 rounded-xl transition-colors font-semibold shadow-sm"
          >
            + Create Transaction
          </button>
          <button
            onClick={handleGenerate}
            disabled={generating}
            className="flex-1 sm:flex-none bg-slate-700 hover:bg-slate-800 text-white text-sm px-5 py-2.5 rounded-xl disabled:opacity-50 transition-colors font-medium"
          >
            {generating ? 'Generating…' : '⚡ Generate 10 Random'}
          </button>
        </div>
      </div>

      {/* Success banner */}
      {successMsg && (
        <div className="bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-xl p-3 text-sm flex items-center justify-between font-medium">
          <span>✓ {successMsg}</span>
          <button onClick={() => setSuccessMsg(null)} className="text-emerald-500 hover:text-emerald-700 text-lg leading-none">&times;</button>
        </div>
      )}

      {/* Filters */}
      <form onSubmit={handleSearch} className="bg-white rounded-2xl border border-slate-200 shadow-sm p-4 space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">Search</label>
            <input value={search} onChange={e => setSearch(e.target.value)}
              placeholder="description / account / payee"
              className="border border-slate-200 rounded-xl px-3 py-2 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-400 bg-slate-50/50" />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">Account ID</label>
            <input value={accountId} onChange={e => setAccountId(e.target.value)}
              className="border border-slate-200 rounded-xl px-3 py-2 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-400 bg-slate-50/50" />
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">Min $</label>
              <input type="number" value={minAmount} onChange={e => setMinAmount(e.target.value)}
                className="border border-slate-200 rounded-xl px-3 py-2 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-400 bg-slate-50/50" />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">Max $</label>
              <input type="number" value={maxAmount} onChange={e => setMaxAmount(e.target.value)}
                className="border border-slate-200 rounded-xl px-3 py-2 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-400 bg-slate-50/50" />
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">From</label>
            <input type="datetime-local" value={from} onChange={e => setFrom(e.target.value)}
              className="border border-slate-200 rounded-xl px-3 py-2 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-400 bg-slate-50/50" />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">To</label>
            <input type="datetime-local" value={to} onChange={e => setTo(e.target.value)}
              className="border border-slate-200 rounded-xl px-3 py-2 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-400 bg-slate-50/50" />
          </div>
        </div>
        <div className="flex gap-2 pt-1">
          <button type="submit"
            className="bg-slate-800 hover:bg-slate-900 text-white text-sm px-5 py-2 rounded-xl font-medium shadow-sm transition-colors">
            Search
          </button>
          <button type="button"
            onClick={() => { setSearch(''); setAccountId(''); setMinAmount(''); setMaxAmount(''); setFrom(''); setTo(''); load(); }}
            className="text-sm text-slate-400 hover:text-slate-600 px-3 py-2 rounded-xl hover:bg-slate-100 transition-colors">
            Clear
          </button>
        </div>
      </form>

      {error && <ErrorMessage message={error} />}

      {loading ? <LoadingSpinner /> : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm min-w-[700px]">
              <thead className="bg-slate-100 border-b-2 border-slate-200">
                <tr className="text-xs font-bold text-slate-600 uppercase tracking-wider">
                  <th className="px-4 py-3 text-left cursor-pointer whitespace-nowrap" onClick={() => toggleSort('id')}>ID <SortIcon field="id" /></th>
                  <th className="px-4 py-3 text-left cursor-pointer whitespace-nowrap" onClick={() => toggleSort('accountId')}>Account <SortIcon field="accountId" /></th>
                  <th className="px-4 py-3 text-left cursor-pointer whitespace-nowrap" onClick={() => toggleSort('payeeId')}>Payee <SortIcon field="payeeId" /></th>
                  <th className="px-4 py-3 text-right cursor-pointer whitespace-nowrap" onClick={() => toggleSort('amount')}>Amount <SortIcon field="amount" /></th>
                  <th className="px-4 py-3 text-left whitespace-nowrap">Type</th>
                  <th className="px-4 py-3 text-left whitespace-nowrap">Status</th>
                  <th className="px-4 py-3 text-left cursor-pointer whitespace-nowrap" onClick={() => toggleSort('timestamp')}>Timestamp <SortIcon field="timestamp" /></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {sorted.length === 0 ? (
                  <tr><td colSpan={7} className="text-center py-12 text-slate-400">No transactions found.</td></tr>
                ) : sorted.map((tx) => (
                  <tr key={tx.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3 font-mono text-slate-500 text-xs font-medium">#{tx.id}</td>
                    <td className="px-4 py-3 font-semibold text-slate-800">{tx.accountId}</td>
                    <td className="px-4 py-3 text-slate-600">{tx.payeeId}</td>
                    <td className="px-4 py-3 text-right font-bold text-slate-800">${Number(tx.amount).toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center gap-1 text-xs px-2.5 py-1 rounded-full font-semibold ring-1 ${
                        tx.type === 'DEBIT'
                          ? 'bg-red-50 text-red-700 ring-red-200'
                          : 'bg-emerald-50 text-emerald-700 ring-emerald-200'
                      }`}>
                        {tx.type}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2.5 py-1 rounded-full font-medium ring-1 ${
                        tx.status === 'COMPLETED' ? 'bg-emerald-50 text-emerald-700 ring-emerald-200' :
                        tx.status === 'PENDING'   ? 'bg-amber-50 text-amber-700 ring-amber-200' :
                                                    'bg-red-50 text-red-600 ring-red-200'
                      }`}>{tx.status}</span>
                    </td>
                    <td className="px-4 py-3 text-slate-500 text-xs whitespace-nowrap font-medium">{new Date(tx.timestamp).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="px-4 py-2.5 border-t border-slate-200 text-xs text-slate-500 bg-slate-50">
            Showing <span className="font-bold text-slate-700">{sorted.length}</span> transaction{sorted.length !== 1 ? 's' : ''}
          </div>
        </div>
      )}

      {showCreate && (
        <CreateTransactionModal onClose={() => setShowCreate(false)} onSaved={handleCreated} />
      )}
    </div>
  );
}
