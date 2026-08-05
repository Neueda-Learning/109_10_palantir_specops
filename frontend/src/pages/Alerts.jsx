import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  getAlerts, acknowledgeAlert, investigateAlert, closeAlert, dismissAlert,
} from '../services/alertService';
import SeverityBadge from '../components/SeverityBadge';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';

const STATUSES = ['OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'CLOSED', 'DISMISSED'];
const SEVERITIES = ['HIGH', 'MEDIUM', 'LOW'];
const rowColor = { HIGH: 'bg-red-50', MEDIUM: 'bg-yellow-50', LOW: 'bg-blue-50' };

/* Small modal for actions that need resolution notes */
function NotesModal({ title, actionLabel, actionClass, onConfirm, onClose }) {
  const [notes, setNotes] = useState('');
  const [saving, setSaving] = useState(false);
  const handle = () => {
    setSaving(true);
    onConfirm(notes).finally(() => setSaving(false));
  };
  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6 space-y-4 border border-slate-100">
        <h3 className="text-base font-bold text-slate-800">{title}</h3>
        <div className="flex flex-col gap-1.5">
          <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">Resolution Notes <span className="normal-case">(optional)</span></label>
          <textarea
            value={notes} onChange={e => setNotes(e.target.value)}
            rows={3}
            className="border border-slate-200 rounded-xl px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-blue-400 bg-slate-50/50"
            placeholder="Add any notes..."
          />
        </div>
        <div className="flex justify-end gap-2">
          <button onClick={onClose}
            className="px-4 py-2 text-sm border border-slate-200 rounded-xl hover:bg-slate-50 transition-colors font-medium text-slate-600">
            Cancel
          </button>
          <button onClick={handle} disabled={saving}
            className={`px-4 py-2 text-sm text-white rounded-xl disabled:opacity-50 font-semibold transition-colors shadow-sm ${actionClass}`}>
            {saving ? 'Saving…' : actionLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function Alerts() {
  const navigate = useNavigate();
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actingId, setActingId] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [modal, setModal] = useState(null); // { type: 'close'|'dismiss', alertId }

  // Filter state
  const [status, setStatus] = useState('');
  const [severity, setSeverity] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  const load = (params = {}) => {
    setLoading(true);
    getAlerts(params)
      .then((r) => setAlerts(r.data))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleFilter = (e) => {
    e.preventDefault();
    const params = {};
    if (status) params.status = status;
    if (severity) params.severity = severity;
    if (from) params.from = from;
    if (to) params.to = to;
    load(params);
  };

  const act = (id, fn) => {
    setActingId(id);
    setActionError(null);
    return fn()
      .then(() => load())
      .catch(e => setActionError(e.response?.data?.message || e.message))
      .finally(() => setActingId(null));
  };

  const handleModalConfirm = (notes) => {
    const { type, alertId } = modal;
    const fn = type === 'close'
      ? () => closeAlert(alertId, notes)
      : () => dismissAlert(alertId, notes);
    return act(alertId, fn).then(() => setModal(null));
  };

  return (
    <div className="p-4 sm:p-6 space-y-5">
      {/* Page header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Alerts</h1>
        <p className="text-sm text-slate-400 mt-0.5">Monitor and action flagged transactions</p>
      </div>

      {/* Filters */}
      <form onSubmit={handleFilter} className="bg-white rounded-2xl border border-slate-100 shadow-sm p-4 space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {[
            { label: 'Status', value: status, onChange: setStatus, options: STATUSES, placeholder: 'All Statuses' },
            { label: 'Severity', value: severity, onChange: setSeverity, options: SEVERITIES, placeholder: 'All Severities' },
          ].map(({ label, value: v, onChange, options, placeholder }) => (
            <div key={label} className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-400 uppercase tracking-wider">{label}</label>
              <select value={v} onChange={e => onChange(e.target.value)}
                className="border border-slate-200 rounded-xl px-3 py-2 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-400 bg-slate-50/50">
                <option value="">{placeholder}</option>
                {options.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
          ))}
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
            Apply Filters
          </button>
          <button type="button"
            onClick={() => { setStatus(''); setSeverity(''); setFrom(''); setTo(''); load(); }}
            className="text-sm text-slate-400 hover:text-slate-600 px-3 py-2 rounded-xl hover:bg-slate-100 transition-colors">
            Clear
          </button>
        </div>
      </form>

      {actionError && <ErrorMessage message={actionError} />}
      {error && <ErrorMessage message={error} />}

      {loading ? <LoadingSpinner /> : (
        <>
          {/* ── Mobile card list (xs only) ── */}
          <div className="sm:hidden space-y-3">
            {alerts.length === 0 ? (
              <div className="text-center py-16 text-slate-400">
                <p className="text-4xl mb-2">🔍</p>
                <p className="text-sm">No alerts found</p>
              </div>
            ) : alerts.map((a) => (
              <div
                key={a.id}
                onClick={() => navigate(`/alerts/${a.id}`)}
                className={`bg-white rounded-2xl border-2 shadow-sm p-4 space-y-3 cursor-pointer active:scale-[0.99] transition-all ${
                  actingId === a.id ? 'opacity-60' : 'hover:shadow-md'
                } ${
                  a.severity === 'HIGH'   ? 'border-red-300'   :
                  a.severity === 'MEDIUM' ? 'border-amber-300' : 'border-blue-300'
                }`}
              >
                <div className="flex items-start justify-between gap-2">
                  <span className="text-xs font-mono text-blue-600 font-bold bg-blue-100 px-2 py-0.5 rounded-md">#{a.id}</span>
                  <div className="flex gap-1.5 flex-wrap justify-end">
                    <SeverityBadge severity={a.severity} />
                    <StatusBadge status={a.status} />
                  </div>
                </div>
                <p className="text-sm font-bold text-slate-900">{a.ruleName}</p>
                <p className="text-xs text-slate-600 line-clamp-2">{a.description}</p>
                <p className="text-xs text-slate-500 font-medium">{new Date(a.createdAt).toLocaleString()}</p>
                <div className="pt-1 border-t border-slate-100" onClick={e => e.stopPropagation()}>
                  <ActionButtons
                    alert={a} acting={actingId === a.id}
                    onAcknowledge={() => act(a.id, () => acknowledgeAlert(a.id))}
                    onInvestigate={() => act(a.id, () => investigateAlert(a.id))}
                    onClose={() => setModal({ type: 'close', alertId: a.id })}
                    onDismiss={() => setModal({ type: 'dismiss', alertId: a.id })}
                  />
                </div>
              </div>
            ))}
            <p className="text-xs text-slate-400 text-center pb-1">{alerts.length} alert{alerts.length !== 1 ? 's' : ''}</p>
          </div>

          {/* ── Desktop table (sm and up) ── */}
          <div className="hidden sm:block bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-sm" style={{minWidth: '720px'}}>
                <thead className="bg-slate-100 border-b-2 border-slate-200">
                  <tr className="text-xs font-bold text-slate-600 uppercase tracking-wider">
                    <th className="px-4 py-3 text-left" style={{width:'60px'}}>ID</th>
                    <th className="px-4 py-3 text-left">Rule</th>
                    <th className="px-4 py-3 text-left" style={{width:'115px'}}>Severity</th>
                    <th className="px-4 py-3 text-left" style={{width:'145px'}}>Status</th>
                    <th className="px-4 py-3 text-left" style={{width:'165px'}}>Created</th>
                    <th className="px-4 py-3 text-left" style={{width:'220px'}}>Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {alerts.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="text-center py-16 text-slate-400">
                        <p className="text-3xl mb-2">🔍</p>
                        <p className="text-sm">No alerts found</p>
                      </td>
                    </tr>
                  ) : alerts.map((a) => (
                    <tr
                      key={a.id}
                      onClick={() => navigate(`/alerts/${a.id}`)}
                      className={`cursor-pointer group transition-colors border-l-4 ${
                        a.severity === 'HIGH'   ? 'border-l-red-400   bg-red-50/40   hover:bg-red-50' :
                        a.severity === 'MEDIUM' ? 'border-l-amber-400 bg-amber-50/40 hover:bg-amber-50' :
                                                  'border-l-blue-400  bg-blue-50/30  hover:bg-blue-50'
                      } ${actingId === a.id ? 'opacity-60' : ''}`}
                    >
                      <td className="px-4 py-3 font-mono text-xs text-blue-600 font-bold group-hover:text-blue-800">#{a.id}</td>
                      <td className="px-4 py-3 font-semibold text-slate-800">{a.ruleName}</td>
                      <td className="px-4 py-3"><SeverityBadge severity={a.severity} /></td>
                      <td className="px-4 py-3"><StatusBadge status={a.status} /></td>
                      <td className="px-4 py-3 text-slate-600 whitespace-nowrap text-xs font-medium">{new Date(a.createdAt).toLocaleString()}</td>
                      <td className="px-4 py-3" onClick={e => e.stopPropagation()}>
                        <ActionButtons
                          alert={a} acting={actingId === a.id}
                          onAcknowledge={() => act(a.id, () => acknowledgeAlert(a.id))}
                          onInvestigate={() => act(a.id, () => investigateAlert(a.id))}
                          onClose={() => setModal({ type: 'close', alertId: a.id })}
                          onDismiss={() => setModal({ type: 'dismiss', alertId: a.id })}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="px-4 py-2.5 border-t border-slate-200 text-xs text-slate-500 bg-slate-50 flex items-center gap-1">
              Showing <span className="font-bold text-slate-700 mx-1">{alerts.length}</span> alert{alerts.length !== 1 ? 's' : ''}
            </div>
          </div>
        </>
      )}

      {modal && (
        <NotesModal
          title={modal.type === 'close' ? 'Close Alert' : 'Dismiss Alert'}
          actionLabel={modal.type === 'close' ? 'Close Alert' : 'Dismiss'}
          actionClass={modal.type === 'close' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-slate-500 hover:bg-slate-600'}
          onConfirm={handleModalConfirm}
          onClose={() => setModal(null)}
        />
      )}
    </div>
  );
}

function ActionButtons({ alert, acting, onAcknowledge, onInvestigate, onClose, onDismiss }) {
  const { status } = alert;
  if (status === 'CLOSED' || status === 'DISMISSED') {
    return (
      <span className="inline-flex items-center gap-1 text-xs text-emerald-600 font-semibold bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-full">
        ✓ Resolved
      </span>
    );
  }
  return (
    <div className="flex flex-wrap gap-1.5">
      {status === 'OPEN' && (
        <button disabled={acting} onClick={onAcknowledge}
          className="text-xs bg-amber-500 hover:bg-amber-600 text-white px-3 py-1 rounded-lg font-semibold disabled:opacity-50 transition-all whitespace-nowrap">
          ✓ Acknowledge
        </button>
      )}
      {status === 'ACKNOWLEDGED' && (
        <button disabled={acting} onClick={onInvestigate}
          className="text-xs bg-orange-500 hover:bg-orange-600 text-white px-3 py-1 rounded-lg font-semibold disabled:opacity-50 transition-all whitespace-nowrap">
          🔍 Investigate
        </button>
      )}
      {status === 'INVESTIGATING' && (
        <button disabled={acting} onClick={onClose}
          className="text-xs bg-emerald-600 hover:bg-emerald-700 text-white px-3 py-1 rounded-lg font-semibold disabled:opacity-50 transition-all whitespace-nowrap">
          ✓ Close
        </button>
      )}
      {(status === 'ACKNOWLEDGED' || status === 'INVESTIGATING') && (
        <button disabled={acting} onClick={onDismiss}
          className="text-xs bg-slate-200 hover:bg-slate-300 text-slate-700 px-3 py-1 rounded-lg font-medium disabled:opacity-50 transition-all whitespace-nowrap">
          ✕ Dismiss
        </button>
      )}
    </div>
  );
}
