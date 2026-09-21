import { useState } from 'react'
import { X, CheckCircle, AlertCircle } from 'lucide-react'
import { bulkUpdateStatus } from '../../api/AutomationApi'
import { useTranslation } from '../../i18n/translations'

export default function BulkStatusModal({ selectedIds = [], onSuccess, onClose }) {
  const t = useTranslation().automation
  const [newStatus, setNewStatus] = useState('')
  const [loading, setLoading] = useState(false)
  const [report, setReport] = useState(null)
  const [error, setError] = useState('')

  const handleApply = async () => {
    if (!newStatus || selectedIds.length === 0) return
    setLoading(true)
    setError('')
    try {
      const res = await bulkUpdateStatus(selectedIds, newStatus)
      setReport(res.data)
      onSuccess?.()
    } catch {
      setError(t.genericError)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      className="bsm-overlay"
      onClick={e => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="bsm-modal">
        <div className="bsm-header">
          <h2 className="bsm-title">{t.bulkTitle}</h2>
          <button className="bsm-close" onClick={onClose}><X size={18} /></button>
        </div>

        <p className="bsm-count">
          <strong>{selectedIds.length}</strong> {selectedIds.length > 1 ? t.selected.plural : t.selected.singular}
        </p>

        {!report ? (
          <>
            <label className="bsm-label">{t.newStatusLabel}</label>
            <select
              className="bsm-select"
              value={newStatus}
              onChange={e => setNewStatus(e.target.value)}
            >
              <option value="">{t.chooseStatus}</option>
              {t.statusOptions.map(s => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
            </select>

            {error && (
              <div className="bsm-error">
                <AlertCircle size={14} /> {error}
              </div>
            )}

            <div className="bsm-actions">
              <button className="bsm-cancel-btn" onClick={onClose} disabled={loading}>
                {t.cancel}
              </button>
              <button
                className="bsm-apply-btn"
                onClick={handleApply}
                disabled={!newStatus || selectedIds.length === 0 || loading}
              >
                {loading ? t.applying : t.apply}
              </button>
            </div>
          </>
        ) : (
          <div className="bsm-report">
            <CheckCircle size={28} className="bsm-check" />
            <p className="bsm-report-title">{t.updateDone}</p>
            <p className="bsm-report-line">
              <strong>{report.updated}</strong> {report.updated > 1 ? t.updated.plural : t.updated.singular}
            </p>
            {report.skipped?.length > 0 && (
              <p className="bsm-report-skip">
                {report.skipped.length} {report.skipped.length > 1 ? t.skipped.plural : t.skipped.singular}
              </p>
            )}
            <button className="bsm-apply-btn" onClick={onClose} style={{ marginTop: 20 }}>
              {t.close}
            </button>
          </div>
        )}
      </div>

      <style>{`
        .bsm-overlay {
          position: fixed; inset: 0; background: rgba(0,0,0,0.55);
          display: flex; align-items: center; justify-content: center;
          z-index: 1000; padding: 16px;
        }
        .bsm-modal {
          background: var(--surface-card); border-radius: 12px;
          padding: 28px; width: 100%; max-width: 440px;
          box-shadow: 0 20px 60px rgba(0,0,0,0.2);
        }
        .bsm-header {
          display: flex; align-items: center; justify-content: space-between;
          margin-bottom: 16px;
        }
        .bsm-title { font-size: 1.1rem; font-weight: 600; margin: 0; color: var(--text-primary); }
        .bsm-close {
          background: none; border: none; cursor: pointer;
          color: var(--text-muted); padding: 4px;
        }
        .bsm-count {
          font-size: 0.9rem; color: var(--text-secondary);
          margin-bottom: 20px;
        }
        .bsm-label {
          display: block; font-size: 0.875rem; margin-bottom: 8px;
          color: var(--text-primary);
        }
        .bsm-select {
          width: 100%; padding: 10px 12px; border: 1.5px solid var(--surface-border);
          border-radius: 8px; font-size: 0.95rem; margin-bottom: 20px;
          box-sizing: border-box; background: var(--surface-card);
          color: var(--text-primary); cursor: pointer;
        }
        .bsm-select:focus { border-color: #6366f1; outline: none; }
        .bsm-error {
          display: flex; align-items: center; gap: 6px;
          color: #c0392b; font-size: 0.85rem; margin-bottom: 12px;
        }
        .bsm-actions {
          display: flex; gap: 10px; justify-content: flex-end;
        }
        .bsm-cancel-btn {
          padding: 10px 20px; border: 1.5px solid var(--surface-border); border-radius: 8px;
          background: none; cursor: pointer; font-size: 0.9rem;
          color: var(--text-secondary);
        }
        .bsm-apply-btn {
          padding: 10px 20px; border: none; border-radius: 8px;
          background: #6366f1; color: #fff;
          cursor: pointer; font-size: 0.9rem; font-weight: 500;
          transition: opacity 0.2s;
        }
        .bsm-apply-btn:disabled { opacity: 0.4; cursor: not-allowed; }
        .bsm-cancel-btn:disabled { opacity: 0.4; cursor: not-allowed; }
        .bsm-report {
          display: flex; flex-direction: column; align-items: center;
          padding: 8px 0; text-align: center;
        }
        .bsm-check { color: #27ae60; margin-bottom: 12px; }
        .bsm-report-title { font-size: 1rem; font-weight: 600; margin: 0 0 8px; color: var(--text-primary); }
        .bsm-report-line { font-size: 0.95rem; color: var(--text-primary); margin: 0 0 4px; }
        .bsm-report-skip { font-size: 0.85rem; color: var(--text-muted); margin: 0; }
      `}</style>
    </div>
  )
}
