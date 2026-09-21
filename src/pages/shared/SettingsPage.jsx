import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  CircleUser, Lock, Mail, Monitor, Shield, SlidersHorizontal,
  ChevronRight, Loader2, CheckCircle,
} from 'lucide-react'
import { useAuthStore }                    from '../../store/AuthStore'
import AppNavbar                           from '../../components/common/AppNavbar'
import TwoFactorSetup                      from '../../features/auth/TwoFactorSetup'
import { getPreferences, savePreferences } from '../../api/PreferencesApi'
import AccountDeletionModal                from '../../features/account/AccountDeletionModal'
import DataExportModal                     from '../../features/account/DataExportModal'
import { getConsents, updateConsent }      from '../../api/GdprApi'
import { useTranslation }                  from '../../i18n/translations'
import './SettingsPage.css'

const DEVICES = [
  { device: 'Chrome Windows', loginDate: '10 juin 2026', ip: '102.244.45.246', city: 'Yaoundé', current: true },
]

export default function SettingsPage() {
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()
  const t = useTranslation().settings
  const [activeSection, setActiveSection] = useState('account')

  const SECTIONS = [
    { id: 'account',       icon: <CircleUser size={18} />,          ...t.sections.account },
    { id: 'security',      icon: <Lock size={18} />,                ...t.sections.security },
    { id: 'communication', icon: <Mail size={18} />,                ...t.sections.communication },
    { id: 'devices',       icon: <Monitor size={18} />,             ...t.sections.devices },
    { id: 'privacy',       icon: <Shield size={18} />,              ...t.sections.privacy },
    { id: 'preferences',   icon: <SlidersHorizontal size={18} />,   ...t.sections.preferences },
  ]

  const handleLogout = () => { logout(); navigate('/login') }
  const [onlineStatus, setOnlineStatus] = useState(true)
  const [readReceipts, setReadReceipts] = useState(true)

  // Preferences state
  const EMPTY_PREF = { preferredJobTypes: '', preferredLocations: '', preferredIndustries: '', salaryExpectationMin: '', salaryExpectationMax: '', notificationsEnabled: true, emailAlertsEnabled: true }
  const [pref,        setPref]        = useState(EMPTY_PREF)
  const [prefLoading, setPrefLoading] = useState(false)
  const [prefSaving,  setPrefSaving]  = useState(false)
  const [prefSaved,   setPrefSaved]   = useState(false)
  const [prefError,   setPrefError]   = useState('')

  const [showDeletionModal, setShowDeletionModal] = useState(false)
  const [showExportModal, setShowExportModal]     = useState(false)
  const [consents, setConsents]                   = useState([])
  const [consentsLoading, setConsentsLoading]     = useState(false)

  useEffect(() => {
    if (activeSection !== 'preferences') return
    setPrefLoading(true)
    getPreferences()
      .then(d => setPref({ preferredJobTypes: d.preferredJobTypes || '', preferredLocations: d.preferredLocations || '', preferredIndustries: d.preferredIndustries || '', salaryExpectationMin: d.salaryExpectationMin ?? '', salaryExpectationMax: d.salaryExpectationMax ?? '', notificationsEnabled: d.notificationsEnabled ?? true, emailAlertsEnabled: d.emailAlertsEnabled ?? true }))
      .catch(() => setPrefError(t.loadPrefError))
      .finally(() => setPrefLoading(false))
  }, [activeSection, t.loadPrefError])

  useEffect(() => {
    if (activeSection === 'privacy') {
      setConsentsLoading(true)
      getConsents()
        .then(res => setConsents(res.data))
        .catch(() => {})
        .finally(() => setConsentsLoading(false))
    }
  }, [activeSection])

  const isConsentActive = (type) =>
    consents.some(c => c.consentType === type && c.accepted)

  const handleConsentToggle = async (type, accepted) => {
    try {
      await updateConsent(type, accepted)
      setConsents(prev =>
        prev.map(c => c.consentType === type ? { ...c, accepted } : c)
          .concat(prev.some(c => c.consentType === type) ? [] : [{ consentType: type, accepted }])
      )
    } catch (_) {}
  }

  const handleSavePref = async () => {
    setPrefSaving(true); setPrefError('')
    try {
      await savePreferences({ ...pref, userId: user?.id, salaryExpectationMin: pref.salaryExpectationMin ? Number(pref.salaryExpectationMin) : null, salaryExpectationMax: pref.salaryExpectationMax ? Number(pref.salaryExpectationMax) : null })
      setPrefSaved(true); setTimeout(() => setPrefSaved(false), 3000)
    } catch { setPrefError(t.savePrefError) }
    finally  { setPrefSaving(false) }
  }

  return (
    <div className="st-shell">

      <AppNavbar />

      {/* ── Body ── */}
      <div className="st-body">

        {/* ── Sidebar ── */}
        <aside className="st-sidebar">
          <h1 className="st-sidebar-title">{t.sidebarTitle}</h1>
          <nav className="st-sidebar-nav">
            {SECTIONS.map(s => (
              <button
                key={s.id}
                className={`st-sidebar-item ${activeSection === s.id ? 'st-sidebar-item--active' : ''}`}
                onClick={() => setActiveSection(s.id)}
              >
                <span className="st-sidebar-icon">{s.icon}</span>
                <div className="st-sidebar-text">
                  <span className="st-sidebar-label">
                    {s.label}
                    {s.badge && <span className="st-badge">{s.badge}</span>}
                  </span>
                  <span className="st-sidebar-sub">{s.sub}</span>
                </div>
                <ChevronRight size={16} className="st-sidebar-arrow" />
              </button>
            ))}
          </nav>
        </aside>

        {/* ── Content ── */}
        <main className="st-content">

          {/* Account settings */}
          {activeSection === 'account' && (
            <div className="st-panel">
              <h2 className="st-panel-title">{t.sections.account.label}</h2>
              <hr className="st-hr" />

              <div className="st-row">
                <div className="st-row-left">
                  <p className="st-row-label">{t.accountType}</p>
                  <p className="st-row-value">{user?.role === 'EMPLOYER' ? t.employer : t.jobSeeker}</p>
                </div>
              </div>
              <hr className="st-hr" />

              <div className="st-row">
                <div className="st-row-left">
                  <p className="st-row-label">{t.email}</p>
                  <p className="st-row-value">{user?.email}</p>
                </div>
              </div>
              <hr className="st-hr" />

              <div className="st-row">
                <div className="st-row-left">
                  <p className="st-row-label">{t.phoneNumber}</p>
                  <p className="st-row-value">{user?.phoneNumber || '—'}</p>
                </div>
              </div>
              <hr className="st-hr" />

              <div className="st-row">
                <p className="st-row-value">{user?.email}</p>
                <button className="st-row-btn" onClick={handleLogout}>{t.logout}</button>
              </div>
              <hr className="st-hr" />

              <button className="st-danger-btn" onClick={() => setShowDeletionModal(true)}>{t.closeAccount}</button>
            </div>
          )}

          {/* Security settings */}
          {activeSection === 'security' && (
            <div className="st-panel">
              <h2 className="st-panel-title">{t.sections.security.label} <span className="st-badge">{t.sections.security.badge}</span></h2>
              <hr className="st-hr" />
              <h3 className="st-sub-title">{t.accountProtection}</h3>
              <TwoFactorSetup />
              <hr className="st-hr" />
              <h3 className="st-sub-title">{t.thirdPartyApps}</h3>
              <hr className="st-hr" />
              <p className="st-empty-text">{t.noThirdPartyApps}</p>
            </div>
          )}

          {/* Communication settings */}
          {activeSection === 'communication' && (
            <div className="st-panel">
              <h2 className="st-panel-title">{t.sections.communication.label}</h2>
              <hr className="st-hr" />

              <div className="st-row st-row--link">
                <p className="st-row-label-big">{t.emailAddress}</p>
                <ChevronRight size={18} className="st-row-arrow" />
              </div>
              <hr className="st-hr" />

              <div className="st-row st-row--link">
                <p className="st-row-label-big">{t.sms}</p>
                <ChevronRight size={18} className="st-row-arrow" />
              </div>
              <hr className="st-hr" />

              <div className="st-toggle-row">
                <div className="st-toggle-text">
                  <p className="st-toggle-label">{t.showOnlineStatus}</p>
                  <p className="st-toggle-sub">{t.showOnlineStatusSub}</p>
                </div>
                <button
                  className={`st-toggle ${onlineStatus ? 'st-toggle--on' : ''}`}
                  onClick={() => setOnlineStatus(v => !v)}
                  aria-label={t.onlineStatusAria}
                >
                  <span className="st-toggle-knob" />
                </button>
              </div>
              <hr className="st-hr" />

              <div className="st-toggle-row">
                <div className="st-toggle-text">
                  <p className="st-toggle-label">{t.showReadReceipts}</p>
                  <p className="st-toggle-sub">{t.showReadReceiptsSub}</p>
                </div>
                <button
                  className={`st-toggle ${readReceipts ? 'st-toggle--on' : ''}`}
                  onClick={() => setReadReceipts(v => !v)}
                  aria-label={t.readReceiptsAria}
                >
                  <span className="st-toggle-knob" />
                </button>
              </div>
              <hr className="st-hr" />
            </div>
          )}

          {/* Devices */}
          {activeSection === 'devices' && (
            <div className="st-panel">
              <h2 className="st-panel-title">{t.sections.devices.label}</h2>
              <hr className="st-hr" />
              <p className="st-devices-hint">{t.devicesHint}</p>
              <table className="st-devices-table">
                <thead>
                  <tr>
                    <th>{t.device}</th>
                    <th>{t.loginDate}</th>
                    <th>{t.ipAddress}</th>
                    <th>{t.actions}</th>
                  </tr>
                </thead>
                <tbody>
                  {DEVICES.map((d, i) => (
                    <tr key={i}>
                      <td>{d.device}</td>
                      <td>{d.loginDate}</td>
                      <td>{d.ip}<br /><span className="st-city">{d.city}</span></td>
                      <td>{d.current && <em className="st-current-device">{t.thisDevice}</em>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Privacy */}
          {activeSection === 'privacy' && (
            <div className="st-panel">
              <h2 className="st-panel-title">{t.sections.privacy.label}</h2>
              <hr className="st-hr" />
              <p className="st-empty-text">{t.privacyProtected}</p>
              <hr className="st-hr" />
              <div className="st-row st-row--link" onClick={() => setShowExportModal(true)} style={{ cursor: 'pointer' }}>
                <p className="st-row-label-big">{t.downloadMyData}</p>
                <ChevronRight size={18} className="st-row-arrow" />
              </div>
              <hr className="st-hr" />
              <div className="st-row st-row--link">
                <p className="st-row-label-big">{t.deleteMyData}</p>
                <ChevronRight size={18} className="st-row-arrow" />
              </div>
              <hr className="st-hr" />

              <div className="st-section">
                <h2 className="st-section-title">{t.privacyConsents}</h2>
                {consentsLoading ? (
                  <div className="st-loading"><Loader2 size={18} className="st-spin" /></div>
                ) : (
                  <>
                    <div className="st-row">
                      <div>
                        <p className="st-row-label-big">{t.analyticsCookies}</p>
                        <p className="st-row-label-small">{t.analyticsCookiesSub}</p>
                      </div>
                      <label className="st-toggle">
                        <input
                          type="checkbox"
                          checked={isConsentActive('ANALYTICS')}
                          onChange={e => handleConsentToggle('ANALYTICS', e.target.checked)}
                        />
                        <span className="st-toggle-slider" />
                      </label>
                    </div>
                    <div className="st-row">
                      <div>
                        <p className="st-row-label-big">{t.marketingComms}</p>
                        <p className="st-row-label-small">{t.marketingCommsSub}</p>
                      </div>
                      <label className="st-toggle">
                        <input
                          type="checkbox"
                          checked={isConsentActive('MARKETING')}
                          onChange={e => handleConsentToggle('MARKETING', e.target.checked)}
                        />
                        <span className="st-toggle-slider" />
                      </label>
                    </div>
                    <div className="st-row">
                      <div>
                        <p className="st-row-label-big">{t.aiProcessing}</p>
                        <p className="st-row-label-small">{t.aiProcessingSub}</p>
                      </div>
                      <label className="st-toggle">
                        <input
                          type="checkbox"
                          checked={isConsentActive('AI_PROCESSING')}
                          onChange={e => handleConsentToggle('AI_PROCESSING', e.target.checked)}
                        />
                        <span className="st-toggle-slider" />
                      </label>
                    </div>
                  </>
                )}
              </div>
            </div>
          )}

          {/* Preferences */}
          {activeSection === 'preferences' && (
            <div className="st-panel">
              <h2 className="st-panel-title">{t.sections.preferences.label}</h2>
              <hr className="st-hr" />
              {prefLoading ? (
                <div style={{ display: 'flex', justifyContent: 'center', padding: 32 }}><Loader2 size={24} className="st-spin" /></div>
              ) : (
                <>
                  {prefError && <p style={{ color: '#c42033', fontSize: 13, marginBottom: 12 }}>{prefError}</p>}
                  <div className="st-pref-grid">
                    <div className="st-pref-field">
                      <label className="st-row-label">{t.desiredContractTypes}</label>
                      <input className="st-pref-input" placeholder={t.contractTypesPlaceholder} value={pref.preferredJobTypes} onChange={e => setPref(p => ({ ...p, preferredJobTypes: e.target.value }))} />
                    </div>
                    <div className="st-pref-field">
                      <label className="st-row-label">{t.preferredLocations}</label>
                      <input className="st-pref-input" placeholder={t.locationsPlaceholder} value={pref.preferredLocations} onChange={e => setPref(p => ({ ...p, preferredLocations: e.target.value }))} />
                    </div>
                    <div className="st-pref-field">
                      <label className="st-row-label">{t.sectors}</label>
                      <input className="st-pref-input" placeholder={t.sectorsPlaceholder} value={pref.preferredIndustries} onChange={e => setPref(p => ({ ...p, preferredIndustries: e.target.value }))} />
                    </div>
                    <div className="st-pref-field">
                      <label className="st-row-label">{t.minSalary}</label>
                      <input className="st-pref-input" type="number" placeholder="150000" value={pref.salaryExpectationMin} onChange={e => setPref(p => ({ ...p, salaryExpectationMin: e.target.value }))} />
                    </div>
                    <div className="st-pref-field">
                      <label className="st-row-label">{t.maxSalary}</label>
                      <input className="st-pref-input" type="number" placeholder="500000" value={pref.salaryExpectationMax} onChange={e => setPref(p => ({ ...p, salaryExpectationMax: e.target.value }))} />
                    </div>
                  </div>
                  <hr className="st-hr" />
                  <div className="st-toggle-row">
                    <div className="st-toggle-text">
                      <p className="st-toggle-label">{t.pushNotifications}</p>
                      <p className="st-toggle-sub">{t.pushNotificationsSub}</p>
                    </div>
                    <button className={`st-toggle ${pref.notificationsEnabled ? 'st-toggle--on' : ''}`} onClick={() => setPref(p => ({ ...p, notificationsEnabled: !p.notificationsEnabled }))} aria-label={t.notificationsAria}>
                      <span className="st-toggle-knob" />
                    </button>
                  </div>
                  <hr className="st-hr" />
                  <div className="st-toggle-row">
                    <div className="st-toggle-text">
                      <p className="st-toggle-label">{t.emailAlerts}</p>
                      <p className="st-toggle-sub">{t.emailAlertsSub}</p>
                    </div>
                    <button className={`st-toggle ${pref.emailAlertsEnabled ? 'st-toggle--on' : ''}`} onClick={() => setPref(p => ({ ...p, emailAlertsEnabled: !p.emailAlertsEnabled }))} aria-label={t.emailAlertsAria}>
                      <span className="st-toggle-knob" />
                    </button>
                  </div>
                  <hr className="st-hr" />
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <button className="st-save-btn" onClick={handleSavePref} disabled={prefSaving}>
                      {prefSaving ? <Loader2 size={14} className="st-spin" /> : null}
                      {prefSaving ? t.saving : t.save}
                    </button>
                    {prefSaved && <span style={{ color: '#166534', fontSize: 13, display: 'flex', alignItems: 'center', gap: 4 }}><CheckCircle size={14} /> {t.saved}</span>}
                  </div>
                </>
              )}
            </div>
          )}

        </main>
      </div>

      {showDeletionModal && <AccountDeletionModal onClose={() => setShowDeletionModal(false)} />}
      {showExportModal && <DataExportModal onClose={() => setShowExportModal(false)} />}
    </div>
  )
}
