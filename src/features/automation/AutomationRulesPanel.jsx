import { useState, useEffect } from 'react'
import { Plus, Zap, ToggleLeft, ToggleRight, AlertCircle } from 'lucide-react'
import { getRules, createRule, toggleRule } from '../../api/AutomationApi'
import { useTranslation } from '../../i18n/translations'

const EMPTY_FORM = {
  name: '', triggerType: '', triggerValue: '',
  actionType: '', actionValue: '', targetStatus: '',
}

export default function AutomationRulesPanel() {
  const t = useTranslation().automation
  const [rules, setRules] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [togglingId, setTogglingId] = useState(null)

  useEffect(() => {
    fetchRules()
  }, [])

  const fetchRules = async () => {
    try {
      const res = await getRules()
      setRules(res.data)
    } catch {
      setError(t.loadRulesError)
    }
  }

  const handleToggle = async (id) => {
    setTogglingId(id)
    try {
      const res = await toggleRule(id)
      setRules(prev => prev.map(r => r.id === id ? res.data : r))
    } catch {
      setError(t.toggleRuleError)
    } finally {
      setTogglingId(null)
    }
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    if (!form.name || !form.triggerType || !form.actionType) return
    setSaving(true)
    setError('')
    try {
      const dto = {
        name: form.name,
        triggerType: form.triggerType,
        triggerValue: form.triggerValue || null,
        actionType: form.actionType,
        actionValue: form.actionValue || null,
        targetStatus: form.targetStatus || null,
      }
      const res = await createRule(dto)
      setRules(prev => [...prev, res.data])
      setForm(EMPTY_FORM)
      setShowForm(false)
    } catch {
      setError(t.createRuleError)
    } finally {
      setSaving(false)
    }
  }

  const triggerValueLabel = form.triggerType === 'DAYS_WITHOUT_ACTION'
    ? t.daysCount
    : form.triggerType === 'STATUS_CHANGED'
      ? t.triggerStatus
      : null

  return (
    <div className="arp-container">
      <div className="arp-header">
        <div className="arp-title-row">
          <Zap size={18} className="arp-icon" />
          <h3 className="arp-title">{t.panelTitle}</h3>
        </div>
        <button className="arp-add-btn" onClick={() => setShowForm(v => !v)}>
          <Plus size={15} /> {t.newRule}
        </button>
      </div>

      {error && (
        <div className="arp-error">
          <AlertCircle size={14} /> {error}
        </div>
      )}

      {showForm && (
        <form className="arp-form" onSubmit={handleCreate}>
          <div className="arp-form-row">
            <div className="arp-field">
              <label className="arp-field-label">{t.ruleName}</label>
              <input
                className="arp-input"
                value={form.name}
                onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                placeholder={t.ruleNamePlaceholder}
                required
              />
            </div>
          </div>

          <div className="arp-form-row arp-two-col">
            <div className="arp-field">
              <label className="arp-field-label">{t.trigger}</label>
              <select
                className="arp-select"
                value={form.triggerType}
                onChange={e => setForm(f => ({ ...f, triggerType: e.target.value, triggerValue: '' }))}
                required
              >
                <option value="">{t.choose}</option>
                {t.triggerTypes.map(tt => (
                  <option key={tt.value} value={tt.value}>{tt.label}</option>
                ))}
              </select>
            </div>

            {triggerValueLabel && (
              <div className="arp-field">
                <label className="arp-field-label">{triggerValueLabel}</label>
                {form.triggerType === 'STATUS_CHANGED' ? (
                  <select
                    className="arp-select"
                    value={form.triggerValue}
                    onChange={e => setForm(f => ({ ...f, triggerValue: e.target.value }))}
                  >
                    <option value="">{t.status}</option>
                    {t.statusOptions.map(s => (
                      <option key={s.value} value={s.value}>{s.label}</option>
                    ))}
                  </select>
                ) : (
                  <input
                    className="arp-input"
                    type="number"
                    min={1}
                    value={form.triggerValue}
                    onChange={e => setForm(f => ({ ...f, triggerValue: e.target.value }))}
                    placeholder="30"
                  />
                )}
              </div>
            )}
          </div>

          <div className="arp-form-row arp-two-col">
            <div className="arp-field">
              <label className="arp-field-label">{t.action}</label>
              <select
                className="arp-select"
                value={form.actionType}
                onChange={e => setForm(f => ({ ...f, actionType: e.target.value, targetStatus: '' }))}
                required
              >
                <option value="">{t.choose}</option>
                {t.actionTypes.map(a => (
                  <option key={a.value} value={a.value}>{a.label}</option>
                ))}
              </select>
            </div>

            {form.actionType === 'CHANGE_STATUS' && (
              <div className="arp-field">
                <label className="arp-field-label">{t.newStatusLabel}</label>
                <select
                  className="arp-select"
                  value={form.targetStatus}
                  onChange={e => setForm(f => ({ ...f, targetStatus: e.target.value }))}
                >
                  <option value="">{t.targetStatus}</option>
                  {t.statusOptions.map(s => (
                    <option key={s.value} value={s.value}>{s.label}</option>
                  ))}
                </select>
              </div>
            )}
          </div>

          <div className="arp-form-actions">
            <button type="button" className="arp-cancel-btn" onClick={() => { setShowForm(false); setForm(EMPTY_FORM) }}>
              {t.cancel}
            </button>
            <button type="submit" className="arp-save-btn" disabled={saving}>
              {saving ? t.saving : t.createRule}
            </button>
          </div>
        </form>
      )}

      {rules.length === 0 && !showForm ? (
        <p className="arp-empty">{t.noRules}</p>
      ) : (
        <ul className="arp-list">
          {rules.map(rule => (
            <li key={rule.id} className={`arp-item ${rule.isActive ? '' : 'arp-item--off'}`}>
              <div className="arp-item-info">
                <span className="arp-item-name">{rule.name}</span>
                <span className="arp-item-meta">
                  {t.triggerTypes.find(tt => tt.value === rule.triggerType)?.label}
                  {rule.triggerValue ? ` (${rule.triggerValue})` : ''}
                  {' → '}
                  {t.actionTypes.find(a => a.value === rule.actionType)?.label}
                </span>
              </div>
              <button
                className="arp-toggle-btn"
                onClick={() => handleToggle(rule.id)}
                disabled={togglingId === rule.id}
                title={rule.isActive ? t.deactivate : t.activate}
              >
                {rule.isActive
                  ? <ToggleRight size={26} className="arp-toggle-on" />
                  : <ToggleLeft size={26} className="arp-toggle-off" />
                }
              </button>
            </li>
          ))}
        </ul>
      )}

      <style>{`
        .arp-container {
          background: var(--surface-card);
          border: 1px solid var(--surface-border);
          border-radius: 12px; padding: 20px;
        }
        .arp-header {
          display: flex; align-items: center; justify-content: space-between;
          margin-bottom: 16px;
        }
        .arp-title-row { display: flex; align-items: center; gap: 8px; }
        .arp-icon { color: #6366f1; }
        .arp-title { font-size: 1rem; font-weight: 600; margin: 0; color: var(--text-primary); }
        .arp-add-btn {
          display: flex; align-items: center; gap: 6px;
          padding: 7px 14px; border: none; border-radius: 8px;
          background: #6366f1; color: #fff;
          font-size: 0.85rem; font-weight: 500; cursor: pointer;
        }
        .arp-error {
          display: flex; align-items: center; gap: 6px;
          color: #c0392b; font-size: 0.85rem; margin-bottom: 12px;
        }
        .arp-form {
          background: var(--surface-page);
          border-radius: 10px; padding: 16px; margin-bottom: 16px;
          border: 1px solid var(--surface-border);
        }
        .arp-form-row { margin-bottom: 12px; }
        .arp-two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
        .arp-field { display: flex; flex-direction: column; gap: 4px; }
        .arp-field-label { font-size: 0.8rem; font-weight: 500; color: var(--text-muted); }
        .arp-input, .arp-select {
          padding: 9px 12px; border: 1.5px solid var(--surface-border); border-radius: 8px;
          font-size: 0.9rem; background: var(--surface-card);
          color: var(--text-primary); box-sizing: border-box; width: 100%;
        }
        .arp-input:focus, .arp-select:focus {
          border-color: #6366f1; outline: none;
        }
        .arp-form-actions {
          display: flex; gap: 10px; justify-content: flex-end; margin-top: 4px;
        }
        .arp-cancel-btn {
          padding: 8px 16px; border: 1.5px solid var(--surface-border); border-radius: 8px;
          background: none; cursor: pointer; font-size: 0.875rem; color: var(--text-secondary);
        }
        .arp-save-btn {
          padding: 8px 16px; border: none; border-radius: 8px;
          background: #6366f1; color: #fff;
          cursor: pointer; font-size: 0.875rem; font-weight: 500;
        }
        .arp-save-btn:disabled { opacity: 0.5; cursor: not-allowed; }
        .arp-empty {
          font-size: 0.875rem; color: var(--text-muted);
          text-align: center; padding: 24px 0;
        }
        .arp-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
        .arp-item {
          display: flex; align-items: center; justify-content: space-between;
          padding: 12px 14px; border: 1px solid var(--surface-border);
          border-radius: 10px; transition: opacity 0.2s;
        }
        .arp-item--off { opacity: 0.5; }
        .arp-item-info { display: flex; flex-direction: column; gap: 2px; }
        .arp-item-name { font-size: 0.9rem; font-weight: 500; color: var(--text-primary); }
        .arp-item-meta { font-size: 0.8rem; color: var(--text-muted); }
        .arp-toggle-btn {
          background: none; border: none; cursor: pointer; padding: 0; line-height: 0;
        }
        .arp-toggle-btn:disabled { opacity: 0.5; cursor: not-allowed; }
        .arp-toggle-on { color: #6366f1; }
        .arp-toggle-off { color: var(--text-muted); }
      `}</style>
    </div>
  )
}
