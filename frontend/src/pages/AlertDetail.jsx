import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getAlert, acknowledgeAlert, investigateAlert, closeAlert, dismissAlert,
} from '../services/alertService';
import SeverityBadge from '../components/SeverityBadge';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';

export default function AlertDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [alert, setAlert] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [notes, setNotes] = useState('');
  const [acting, setActing] = useState(false);

  const load = () => {
    setLoading(true);
    getAlert(id)
      .then(r => setAlert(r.data))
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [id]);

  const act = (fn) => {
    setActing(true);
    setActionError(null);
    fn()
      .then(() => load())
      .catch(e => setActionError(e.response?.data?.message || e.message))
      .finally(() => setActing(false));
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;
  if (!alert) return null;

  const { status } = alert;

  return (
    <div className="p-4 sm:p-6 space-y-5">
      {/* Back button */}
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-800 font-medium transition-colors group"
      >
        <span className="group-hover:-translate-x-0.5 transition-transform">←</span> Back to Alerts
      </button>

      {/* Alert Info */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5 sm:p-6 space-y-4">
        <div className="flex items-start justify-between flex-wrap gap-3">
          <div>
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-widest mb-1">Alert</p>
            <h1 className="text-2xl font-bold text-slate-800 tracking-tight">#{alert.id}</h1>
          </div>
          <div className="flex gap-2 flex-wrap">
            <SeverityBadge severity={alert.severity} />
            <StatusBadge status={alert.status} />
          </div>
        </div>
        <p className="text-slate-600 text-sm leading-relaxed bg-slate-50 rounded-xl p-3 border border-slate-100">{alert.description}</p>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 text-sm text-slate-600">
          <div className="rounded-xl p-3 border border-slate-100 bg-slate-50/50">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">Rule</span>
            <span className="font-semibold text-slate-800">{alert.ruleName}</span>
          </div>
          <div className="rounded-xl p-3 border border-slate-100 bg-slate-50/50">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">Created</span>
            <span>{new Date(alert.createdAt).toLocaleString()}</span>
          </div>
          {alert.acknowledgedAt && (
            <div className="rounded-xl p-3 border border-amber-100 bg-amber-50/50">
              <span className="text-xs font-semibold text-amber-500 uppercase tracking-wider block mb-1">Acknowledged</span>
              <span>{new Date(alert.acknowledgedAt).toLocaleString()}</span>
            </div>
          )}
          {alert.closedAt && (
            <div className="rounded-xl p-3 border border-emerald-100 bg-emerald-50/50">
              <span className="text-xs font-semibold text-emerald-500 uppercase tracking-wider block mb-1">Closed</span>
              <span>{new Date(alert.closedAt).toLocaleString()}</span>
            </div>
          )}
          {alert.resolutionNotes && (
            <div className="rounded-xl p-3 border border-blue-100 bg-blue-50/50 col-span-full">
              <span className="text-xs font-semibold text-blue-500 uppercase tracking-wider block mb-1">Resolution Notes</span>
              <span>{alert.resolutionNotes}</span>
            </div>
          )}
        </div>
      </div>

      {/* Action Buttons */}
      {['OPEN', 'ACKNOWLEDGED', 'INVESTIGATING'].includes(status) && (
        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5 space-y-4">
          <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider">Actions</h2>
          {actionError && <ErrorMessage message={actionError} />}
          {(status === 'ACKNOWLEDGED' || status === 'INVESTIGATING') && (
            <div>
              <label className="text-xs font-medium text-slate-400 uppercase tracking-wider block mb-1.5">Resolution Notes</label>
              <textarea
                value={notes}
                onChange={e => setNotes(e.target.value)}
                rows={3}
                className="w-full border border-slate-200 rounded-xl px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-blue-400 bg-slate-50/50"
                placeholder="Optional notes..."
              />
            </div>
          )}
          <div className="flex flex-wrap gap-2 sm:gap-3">
            {status === 'OPEN' && (
              <button disabled={acting} onClick={() => act(() => acknowledgeAlert(id))}
                className="flex-1 sm:flex-none bg-amber-500 hover:bg-amber-600 text-white text-sm px-5 py-2.5 rounded-xl disabled:opacity-50 font-semibold shadow-sm shadow-amber-200 transition-all">
                ✓ Acknowledge
              </button>
            )}
            {status === 'ACKNOWLEDGED' && (
              <button disabled={acting} onClick={() => act(() => investigateAlert(id))}
                className="flex-1 sm:flex-none bg-orange-500 hover:bg-orange-600 text-white text-sm px-5 py-2.5 rounded-xl disabled:opacity-50 font-semibold shadow-sm shadow-orange-200 transition-all">
                🔍 Investigate
              </button>
            )}
            {status === 'INVESTIGATING' && (
              <button disabled={acting} onClick={() => act(() => closeAlert(id, notes))}
                className="flex-1 sm:flex-none bg-emerald-600 hover:bg-emerald-700 text-white text-sm px-5 py-2.5 rounded-xl disabled:opacity-50 font-semibold shadow-sm shadow-emerald-200 transition-all">
                ✅ Close Alert
              </button>
            )}
            {(status === 'ACKNOWLEDGED' || status === 'INVESTIGATING') && (
              <button disabled={acting} onClick={() => act(() => dismissAlert(id, notes))}
                className="flex-1 sm:flex-none bg-slate-100 hover:bg-slate-200 text-slate-600 text-sm px-5 py-2.5 rounded-xl disabled:opacity-50 font-medium transition-all border border-slate-200">
                ✕ Dismiss (False Positive)
              </button>
            )}
          </div>
        </div>
      )}

      {/* Related Transactions */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-slate-100">
          <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider">Related Transactions</h2>
        </div>
        {!alert.transactions?.length ? (
          <p className="text-sm text-slate-400 px-5 py-6">No linked transactions.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm min-w-[480px]">
              <thead className="bg-slate-50 border-b border-slate-100">
                <tr className="text-xs font-semibold text-slate-400 uppercase tracking-wider text-left">
                  <th className="px-5 py-3">ID</th>
                  <th className="px-5 py-3">Account</th>
                  <th className="px-5 py-3">Payee</th>
                  <th className="px-5 py-3 text-right">Amount</th>
                  <th className="px-5 py-3">Type</th>
                  <th className="px-5 py-3">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {alert.transactions?.map(tx => (
                  <tr key={tx.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-5 py-3 font-mono text-slate-400 text-xs">#{tx.id}</td>
                    <td className="px-5 py-3 font-semibold text-slate-700">{tx.accountId}</td>
                    <td className="px-5 py-3 text-slate-600">{tx.payeeId}</td>
                    <td className="px-5 py-3 text-right font-bold text-slate-800">${Number(tx.amount).toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
                    <td className="px-5 py-3">
                      <span className={`text-xs px-2.5 py-1 rounded-full font-semibold ring-1 ${tx.type === 'DEBIT' ? 'bg-red-50 text-red-600 ring-red-200' : 'bg-emerald-50 text-emerald-600 ring-emerald-200'}`}>
                        {tx.type}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-slate-400 text-xs whitespace-nowrap">{new Date(tx.timestamp).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Status History Timeline */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5 sm:p-6">
        <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-5">Status History</h2>
        <ol className="relative border-l-2 border-blue-100 space-y-5 pl-6">
          {alert.history?.map((h, i) => (
            <li key={i} className="relative">
              <div className="absolute -left-[1.4rem] top-1 w-4 h-4 rounded-full bg-blue-500 border-2 border-white shadow-md shadow-blue-200" />
              <p className="text-sm font-semibold text-slate-800">
                {h.previousStatus
                  ? <><span className="text-slate-400">{h.previousStatus}</span><span className="text-slate-300 mx-1.5">→</span><span className="text-blue-600">{h.newStatus}</span></>
                  : <>Created as <span className="text-blue-600">{h.newStatus}</span></>
                }
              </p>
              {h.notes && <p className="text-xs text-slate-500 mt-0.5 italic bg-slate-50 rounded-lg px-2 py-1 mt-1 inline-block">{h.notes}</p>}
              <p className="text-xs text-slate-400 mt-1">{new Date(h.changedAt).toLocaleString()}</p>
            </li>
          ))}
        </ol>
      </div>
    </div>
  );
}
