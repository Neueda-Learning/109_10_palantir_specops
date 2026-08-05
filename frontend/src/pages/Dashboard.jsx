import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import { getDashboardStats } from '../services/dashboardService';
import { getAlerts } from '../services/alertService';
import SeverityBadge from '../components/SeverityBadge';
import StatusBadge from '../components/StatusBadge';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorMessage from '../components/ErrorMessage';

const SEVERITY_COLORS = { HIGH: '#ef4444', MEDIUM: '#eab308', LOW: '#3b82f6' };
const STATUS_COLORS = {
  OPEN: '#ef4444', ACKNOWLEDGED: '#eab308', INVESTIGATING: '#f97316',
  CLOSED: '#22c55e', DISMISSED: '#6b7280',
};

function StatCard({ label, value, icon, gradient, sub }) {
  return (
    <div className={`relative overflow-hidden rounded-2xl p-5 shadow-lg ${gradient}`}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-white/70 mb-2">{label}</p>
          <p className="text-4xl font-extrabold text-white leading-none">{value ?? 0}</p>
          {sub && <p className="text-xs text-white/60 mt-1.5">{sub}</p>}
        </div>
        <span className="text-4xl opacity-25 select-none">{icon}</span>
      </div>
      {/* Decorative circle */}
      <div className="absolute -bottom-4 -right-4 w-20 h-20 rounded-full bg-white/10" />
    </div>
  );
}

export default function Dashboard() {
  const navigate = useNavigate();
  const [stats, setStats] = useState(null);
  const [recentAlerts, setRecentAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([getDashboardStats(), getAlerts({})])
      .then(([statsRes, alertsRes]) => {
        setStats(statsRes.data);
        setRecentAlerts(alertsRes.data.slice(0, 10));
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} />;

  const severityData = [
    { name: 'HIGH', count: recentAlerts.filter(a => a.severity === 'HIGH').length },
    { name: 'MEDIUM', count: recentAlerts.filter(a => a.severity === 'MEDIUM').length },
    { name: 'LOW', count: recentAlerts.filter(a => a.severity === 'LOW').length },
  ];

  const statusData = ['OPEN', 'ACKNOWLEDGED', 'INVESTIGATING', 'CLOSED', 'DISMISSED'].map(s => ({
    name: s,
    value: stats[s.toLowerCase() + 'Count'] || 0,
  })).filter(d => d.value > 0);

  return (
    <div className="p-4 sm:p-6 space-y-6">
      {/* Page header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Dashboard</h1>
        <p className="text-sm text-slate-400 mt-0.5">Real-time transaction monitoring overview</p>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        <StatCard label="Open Alerts" value={stats.openCount} icon="🚨"
          gradient="bg-gradient-to-br from-red-500 to-rose-600 shadow-red-200"
          sub="Requires attention" />
        <StatCard label="Acknowledged" value={stats.acknowledgedCount} icon="👁️"
          gradient="bg-gradient-to-br from-amber-400 to-orange-500 shadow-amber-200"
          sub="Under review" />
        <StatCard label="Alerts Today" value={stats.alertsToday} icon="📅"
          gradient="bg-gradient-to-br from-blue-500 to-indigo-600 shadow-blue-200"
          sub="Last 24 hours" />
        <StatCard label="Avg Resolution" value={stats.avgResolutionMinutes != null ? `${Math.round(stats.avgResolutionMinutes)}m` : '—'} icon="⏱️"
          gradient="bg-gradient-to-br from-emerald-500 to-teal-600 shadow-emerald-200"
          sub="Minutes to close" />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5">
          <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-4">Alerts by Severity</h2>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={severityData} barCategoryGap="40%">
              <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
              <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
              <Tooltip contentStyle={{ borderRadius: 12, border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.1)', fontSize: 12 }} />
              <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                {severityData.map((entry) => (
                  <Cell key={entry.name} fill={SEVERITY_COLORS[entry.name]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5">
          <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-4">Status Distribution</h2>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie data={statusData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={72} innerRadius={32} paddingAngle={3}>
                {statusData.map((entry) => (
                  <Cell key={entry.name} fill={STATUS_COLORS[entry.name]} />
                ))}
              </Pie>
              <Legend wrapperStyle={{ fontSize: 11 }} />
              <Tooltip contentStyle={{ borderRadius: 12, border: 'none', boxShadow: '0 4px 20px rgba(0,0,0,0.1)', fontSize: 12 }} />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Recent Alerts */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
        <div className="px-5 pt-5 pb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider">Recent Alerts</h2>
          <Link to="/alerts" className="text-xs text-blue-500 hover:text-blue-700 font-medium transition-colors">View all →</Link>
        </div>
        {recentAlerts.length === 0 ? (
          <p className="text-slate-400 text-sm px-5 pb-5">No alerts yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm min-w-[500px]">
              <thead className="bg-slate-50 border-y border-slate-100">
                <tr className="text-left text-slate-400 text-xs uppercase tracking-wider">
                  <th className="px-5 py-3">ID</th>
                  <th className="px-5 py-3">Rule</th>
                  <th className="px-5 py-3">Severity</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3 whitespace-nowrap">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {recentAlerts.map((a) => (
                  <tr key={a.id} onClick={() => navigate(`/alerts/${a.id}`)} className="hover:bg-blue-50/40 cursor-pointer transition-colors group">
                    <td className="px-5 py-3 font-mono text-xs text-blue-500 group-hover:text-blue-700 font-semibold">#{a.id}</td>
                    <td className="px-5 py-3 text-slate-700 font-medium">{a.ruleName}</td>
                    <td className="px-5 py-3"><SeverityBadge severity={a.severity} /></td>
                    <td className="px-5 py-3"><StatusBadge status={a.status} /></td>
                    <td className="px-5 py-3 text-slate-400 text-xs whitespace-nowrap">{new Date(a.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
