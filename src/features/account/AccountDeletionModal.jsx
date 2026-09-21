import { useState } from 'react'
import { X, AlertTriangle } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/AuthStore'
import { deleteMyAccount } from '../../api/GdprApi'
import { useTranslation } from '../../i18n/translations'

export default function AccountDeletionModal({ onClose }) {
  const t = useTranslation().accountDeletion
  const [confirmText, setConfirmText] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const { logout } = useAuthStore()
  const navigate = useNavigate()

  const isConfirmed = confirmText === t.confirmWord

  const handleDelete = async () => {
    if (!isConfirmed) return
    setLoading(true)
    setError('')
    try {
      await deleteMyAccount()
      logout()
      navigate('/login')
    } catch (e) {
      setError(t.genericError)
      setLoading(false)
    }
  }

  return (
    <div
      className="adm-overlay"
      onClick={e => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="adm-modal">
        <div className="adm-header">
          <h2 className="adm-title">{t.title}</h2>
          <button className="adm-close" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="adm-warning">
          <AlertTriangle size={20} />
          <span>{t.warning}</span>
        </div>

        <p className="adm-desc">
          {t.desc}
        </p>

        <label className="adm-label">
          {t.confirmLabel} <strong>{t.confirmWord}</strong> {t.confirmLabelSuffix}
        </label>
        <input
          className="adm-input"
          type="text"
          value={confirmText}
          onChange={e => setConfirmText(e.target.value)}
          placeholder={t.confirmWord}
          autoFocus
        />

        {error && <p className="adm-error">{error}</p>}

        <div className="adm-actions">
          <button className="adm-cancel-btn" onClick={onClose} disabled={loading}>
            {t.cancel}
          </button>
          <button
            className="adm-delete-btn"
            onClick={handleDelete}
            disabled={!isConfirmed || loading}
          >
            {loading ? t.deleting : t.deletePermanently}
          </button>
        </div>
      </div>

      <style>{`
        .adm-overlay {
          position: fixed; inset: 0; background: rgba(0,0,0,0.55);
          display: flex; align-items: center; justify-content: center;
          z-index: 1000; padding: 16px;
        }
        .adm-modal {
          background: var(--surface-card); border-radius: 12px;
          padding: 28px; width: 100%; max-width: 460px;
          box-shadow: 0 20px 60px rgba(0,0,0,0.2);
        }
        .adm-header {
          display: flex; align-items: center; justify-content: space-between;
          margin-bottom: 16px;
        }
        .adm-title { font-size: 1.1rem; font-weight: 600; margin: 0; color: var(--text-primary); }
        .adm-close {
          background: none; border: none; cursor: pointer;
          color: var(--text-muted); padding: 4px;
        }
        .adm-warning {
          display: flex; align-items: center; gap: 8px;
          background: #fff3f3; color: #c0392b; border: 1px solid #f5c6c6;
          border-radius: 8px; padding: 12px 14px; margin-bottom: 16px;
          font-size: 0.9rem; font-weight: 500;
        }
        .adm-desc {
          color: var(--text-secondary); font-size: 0.875rem;
          line-height: 1.6; margin-bottom: 20px;
        }
        .adm-label {
          display: block; font-size: 0.875rem; margin-bottom: 8px;
          color: var(--text-primary);
        }
        .adm-input {
          width: 100%; padding: 10px 12px; border: 1.5px solid var(--surface-border);
          border-radius: 8px; font-size: 0.95rem; margin-bottom: 16px;
          box-sizing: border-box; outline: none;
          font-family: monospace; letter-spacing: 1px;
          background: var(--surface-card); color: var(--text-primary);
        }
        .adm-input:focus { border-color: #e74c3c; }
        .adm-error {
          color: #c0392b; font-size: 0.85rem; margin-bottom: 12px;
        }
        .adm-actions {
          display: flex; gap: 10px; justify-content: flex-end;
        }
        .adm-cancel-btn {
          padding: 10px 20px; border: 1.5px solid var(--surface-border); border-radius: 8px;
          background: none; cursor: pointer; font-size: 0.9rem;
          color: var(--text-primary);
        }
        .adm-delete-btn {
          padding: 10px 20px; border: none; border-radius: 8px;
          background: #e74c3c; color: #fff; cursor: pointer;
          font-size: 0.9rem; font-weight: 500;
          opacity: 1; transition: opacity 0.2s;
        }
        .adm-delete-btn:disabled { opacity: 0.4; cursor: not-allowed; }
        .adm-cancel-btn:disabled { opacity: 0.4; cursor: not-allowed; }
      `}</style>
    </div>
  )
}
