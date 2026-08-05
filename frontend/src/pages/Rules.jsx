import { useEffect, useState } from 'react';
import {
  getRules, createRule, updateRule, deleteRule, activateRule, deactivateRule,
} from '../services/ruleService';
import SeverityBadge from '../components/SeverityBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';

const RULE_TYPES = ['AMOUNT_THRESHOLD', 'VELOCITY', 'NEW_PAYEE', 'DAILY_LIMIT'];
const SEVERITIES = ['HIGH', 'MEDIUM', 'LOW'];

const emptyForm = { name: '', description: '', type: 'AMOUNT_THRESHOLD', severity: 'MEDIUM', active: true, thresholdAmount: '', transactionCount: '', timeWindowMinutes: '', dailyLimit: '' };

function RuleModal({ onClose, onSave, initial }) {
  const [form, setForm] = useState(initial || emptyForm);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const handleSubmit = (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    const payload = {
      name: form.name,
      description: form.description,
      type: form.type,
      severity: form.severity,
      active: form.active,
      thresholdAmount: form.thresholdAmount !== '' ? Number(form.thresholdAmount) : null,
      transactionCount: form.transactionCount !== '' ? Number(form.transactionCount) : null,
      timeWindowMinutes: form.timeWindowMinutes !== '' ? Number(form.timeWindowMinutes) : null,
      dailyLimit: form.dailyLimit !== '' ? Number(form.dailyLimit) : null,
    };
    const op = initial?.id ? updateRule(initial.id, payload) : createRule(payload);
    op.then(() => onSave())
      .catch(e => setError(e.response?.data?.message || e.message))
      .finally(() => setSaving(false));
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg p-5 sm:p-6 space-y-4 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-gray-800">{initial?.id ? 'Edit Rule' : 'Add Rule'}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl leading-none">&times;</button>
        </div>
        {error && <ErrorMessage message={error} />}
        <form onSubmit={handleSubmit} className="space-y-3">
          <Field label="Name" required>
            <input required value={form.name} onChange={e => set('name', e.target.value)}
              className="w-full border rounded px-3 py-1.5 text-sm" />
          </Field>
          <Field label="Description">
            <input value={form.description} onChange={e => set('description', e.target.value)}
              className="w-full border rounded px-3 py-1.5 text-sm" />
          </Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Type">
              <select value={form.type} onChange={e => set('type', e.target.value)}
                className="w-full border rounded px-3 py-1.5 text-sm">
                {RULE_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </Field>
            <Field label="Severity">
              <select value={form.severity} onChange={e => set('severity', e.target.value)}
                className="w-full border rounded px-3 py-1.5 text-sm">
                {SEVERITIES.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </Field>
          </div>

          {/* Dynamic fields */}
          {form.type === 'AMOUNT_THRESHOLD' && (
            <Field label="Threshold Amount ($)">
              <input type="number" step="0.01" value={form.thresholdAmount} onChange={e => set('thresholdAmount', e.target.value)}
                className="w-full border rounded px-3 py-1.5 text-sm" />
            </Field>
          )}
          {form.type === 'VELOCITY' && (
            <div className="grid grid-cols-2 gap-3">
              <Field label="Max Transactions">
                <input type="number" value={form.transactionCount} onChange={e => set('transactionCount', e.target.value)}
                  className="w-full border rounded px-3 py-1.5 text-sm" />
              </Field>
              <Field label="Time Window (minutes)">
                <input type="number" value={form.timeWindowMinutes} onChange={e => set('timeWindowMinutes', e.target.value)}
                  className="w-full border rounded px-3 py-1.5 text-sm" />
              </Field>
            </div>
          )}
          {form.type === 'DAILY_LIMIT' && (
            <Field label="Daily Limit ($)">
              <input type="number" step="0.01" value={form.dailyLimit} onChange={e => set('dailyLimit', e.target.value)}
                className="w-full border rounded px-3 py-1.5 text-sm" />
            </Field>
          )}

          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" checked={form.active} onChange={e => set('active', e.target.checked)} className="w-4 h-4" />
            Active
          </label>

          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onClose}
              className="px-4 py-2 text-sm rounded-lg border hover:bg-gray-50">Cancel</button>
            <button type="submit" disabled={saving}
              className="px-4 py-2 text-sm rounded-lg bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50">
              {saving ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function Field({ label, children }) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-xs text-gray-500">{label}</label>
      {children}
    </div>
  );
}

function ruleParams(rule) {
  if (rule.type === 'AMOUNT_THRESHOLD') return `> $${rule.thresholdAmount}`;
  if (rule.type === 'VELOCITY') return `> ${rule.transactionCount} txns / ${rule.timeWindowMinutes} min`;
  if (rule.type === 'DAILY_LIMIT') return `> $${rule.dailyLimit} / day`;
  return '—';
}

export default function Rules() {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editRule, setEditRule] = useState(null);

  const load = () => {
    setLoading(true);
    getRules()
      .then(r => setRules(r.data))
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleSave = () => { setModalOpen(false); setEditRule(null); load(); };

  const handleToggle = (rule) => {
    const op = rule.active ? deactivateRule(rule.id) : activateRule(rule.id);
    op.then(load).catch(e => setError(e.message));
  };

  const handleDelete = (id) => {
    if (!confirm('Delete this rule?')) return;
    deleteRule(id).then(load).catch(e => setError(e.message));
  };

  return (
    <div className="p-4 sm:p-6 space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Monitoring Rules</h1>
          <p className="text-sm text-slate-400 mt-0.5">Define and manage transaction alert rules</p>
        </div>
        <button onClick={() => { setEditRule(null); setModalOpen(true); }}
          className="w-full sm:w-auto bg-blue-600 hover:bg-blue-700 text-white text-sm px-5 py-2.5 rounded-xl transition-colors font-semibold shadow-sm">
          + Add Rule
        </button>
      </div>

      {error && <ErrorMessage message={error} />}
      {loading ? <LoadingSpinner /> : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm min-w-[600px]">
              <thead className="bg-slate-100 border-b-2 border-slate-200">
                <tr className="text-xs font-bold text-slate-600 uppercase tracking-wider">
                  <th className="px-4 py-3 text-left">Name</th>
                  <th className="px-4 py-3 text-left">Type</th>
                  <th className="px-4 py-3 text-left">Severity</th>
                  <th className="px-4 py-3 text-left">Parameters</th>
                  <th className="px-4 py-3 text-center">Active</th>
                  <th className="px-4 py-3 text-left">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {rules.length === 0 ? (
                  <tr><td colSpan={6} className="text-center py-12 text-slate-400">No rules defined.</td></tr>
                ) : rules.map(rule => (
                  <tr key={rule.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3">
                      <p className="font-semibold text-slate-800">{rule.name}</p>
                      {rule.description && <p className="text-xs text-slate-400 mt-0.5 truncate max-w-xs">{rule.description}</p>}
                    </td>
                    <td className="px-4 py-3 text-slate-600 font-mono text-xs whitespace-nowrap">{rule.type}</td>
                    <td className="px-4 py-3"><SeverityBadge severity={rule.severity} /></td>
                    <td className="px-4 py-3 text-slate-600 text-xs whitespace-nowrap">{ruleParams(rule)}</td>
                    <td className="px-4 py-3 text-center">
                      <button
                        onClick={() => handleToggle(rule)}
                        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none ${
                          rule.active ? 'bg-emerald-500' : 'bg-slate-300'
                        }`}
                        title={rule.active ? 'Click to deactivate' : 'Click to activate'}
                      >
                        <span className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${
                          rule.active ? 'translate-x-6' : 'translate-x-1'
                        }`} />
                      </button>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        <button
                          onClick={() => { setEditRule(rule); setModalOpen(true); }}
                          className="text-xs bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200 px-2.5 py-1 rounded-lg font-semibold transition-colors"
                        >Edit</button>
                        <button
                          onClick={() => handleDelete(rule.id)}
                          className="text-xs bg-red-50 hover:bg-red-100 text-red-600 border border-red-200 px-2.5 py-1 rounded-lg font-medium transition-colors"
                        >Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="px-4 py-2.5 border-t border-slate-200 text-xs text-slate-500 bg-slate-50">
            {rules.length} rule{rules.length !== 1 ? 's' : ''}
          </div>
        </div>
      )}

      {modalOpen && (
        <RuleModal
          onClose={() => { setModalOpen(false); setEditRule(null); }}
          onSave={handleSave}
          initial={editRule ? {
            ...editRule,
            thresholdAmount: editRule.thresholdAmount ?? '',
            transactionCount: editRule.transactionCount ?? '',
            timeWindowMinutes: editRule.timeWindowMinutes ?? '',
            dailyLimit: editRule.dailyLimit ?? '',
          } : null}
        />
      )}
    </div>
  );
}
