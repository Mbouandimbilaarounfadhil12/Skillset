import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Users } from 'lucide-react'
import { getTalentPools } from '../../api/TalentPoolApi'
import TalentPoolCreateModal from './TalentPoolCreateModal'
import AppNavbar from '../../components/common/AppNavbar'
import { usePreferencesStore } from '../../store/PreferencesStore'
import { useTranslation } from '../../i18n/translations'

export default function TalentPoolList() {
  const navigate = useNavigate()
  const { language } = usePreferencesStore()
  const t = useTranslation().talentPool
  const [pools, setPools] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showCreateForm, setShowCreateForm] = useState(false)

  useEffect(() => {
    loadPools()
  }, [])

  const loadPools = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await getTalentPools()
      setPools(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Error loading talent pools:', err)
      setError(t.loadError)
      setPools([])
    } finally {
      setLoading(false)
    }
  }

  const handleCreateSuccess = () => {
    setShowCreateForm(false)
    loadPools()
  }

  const formatDate = (dateString) => {
    if (!dateString) return ''
    const date = new Date(dateString)
    return date.toLocaleDateString(language === 'fr' ? 'fr-FR' : 'en-US', { year: 'numeric', month: 'long', day: 'numeric' })
  }

  return (
    <div style={{ minHeight: '100vh', background: 'var(--surface-page)' }}>
      <AppNavbar />

      <main style={{ maxWidth: '1200px', margin: '0 auto', padding: '40px 20px' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '40px' }}>
          <div>
            <h1 style={{ fontSize: '28px', fontWeight: 700, color: 'var(--text-primary)', margin: '0 0 8px 0', display: 'flex', alignItems: 'center', gap: '12px' }}>
              <Users size={28} />
              {t.title}
            </h1>
            <p style={{ fontSize: '14px', color: 'var(--text-secondary)', margin: 0 }}>
              {t.subtitle}
            </p>
          </div>
          <button
            onClick={() => setShowCreateForm(true)}
            style={{
              display: 'flex', alignItems: 'center', gap: '8px',
              padding: '12px 24px', borderRadius: '8px', fontSize: '14px', fontWeight: 600,
              backgroundColor: '#c42033', color: '#fff', border: 'none', cursor: 'pointer',
              transition: 'all 0.2s', boxShadow: '0 2px 8px rgba(196,32,51,0.2)'
            }}
            onMouseEnter={(e) => e.target.style.boxShadow = '0 4px 12px rgba(196,32,51,0.3)'}
            onMouseLeave={(e) => e.target.style.boxShadow = '0 2px 8px rgba(196,32,51,0.2)'}
          >
            <Plus size={16} /> {t.createPool}
          </button>
        </div>

        {/* Modal */}
        {showCreateForm && (
          <TalentPoolCreateModal
            onClose={() => setShowCreateForm(false)}
            onSuccess={handleCreateSuccess}
          />
        )}

        {/* Loading */}
        {loading && (
          <div style={{ textAlign: 'center', padding: '60px 20px' }}>
            <div style={{
              display: 'inline-block', width: '32px', height: '32px',
              border: '3px solid var(--surface-border)', borderTop: '3px solid #c42033', borderRadius: '50%',
              animation: 'spin 0.8s linear infinite'
            }} />
            <p style={{ marginTop: '16px', color: 'var(--text-secondary)' }}>{t.loading}</p>
            <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
          </div>
        )}

        {/* Error */}
        {error && !loading && (
          <div style={{
            padding: '16px 20px', borderRadius: '8px', backgroundColor: '#fee',
            border: '1px solid #fcc', color: '#c42033', textAlign: 'center'
          }}>
            {error}
          </div>
        )}

        {/* Empty State */}
        {!loading && !error && pools.length === 0 && (
          <div style={{
            textAlign: 'center', padding: '80px 20px',
            borderRadius: '12px', background: 'var(--surface-card)'
          }}>
            <Users size={48} style={{ color: 'var(--text-muted)', marginBottom: '16px' }} />
            <h3 style={{ fontSize: '18px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '8px' }}>
              {t.emptyTitle}
            </h3>
            <p style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '24px' }}>
              {t.emptySub}
            </p>
            <button
              onClick={() => setShowCreateForm(true)}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: '8px',
                padding: '12px 24px', borderRadius: '8px', fontSize: '14px', fontWeight: 600,
                backgroundColor: '#c42033', color: '#fff', border: 'none', cursor: 'pointer'
              }}
            >
              <Plus size={16} /> {t.createPool}
            </button>
          </div>
        )}

        {/* Grid */}
        {!loading && !error && pools.length > 0 && (
          <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
            gap: '20px'
          }}>
            {pools.map(pool => (
              <div
                key={pool.id}
                onClick={() => navigate(`/employer/talent-pools/${pool.id}`)}
                style={{
                  padding: '20px', borderRadius: '12px', background: 'var(--surface-card)',
                  border: '1px solid var(--surface-border)', cursor: 'pointer', transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.08)'
                  e.currentTarget.style.transform = 'translateY(-2px)'
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.boxShadow = 'none'
                  e.currentTarget.style.transform = 'translateY(0)'
                }}
              >
                <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-primary)', margin: '0 0 8px 0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {pool.name}
                </h3>
                <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: '0 0 12px 0', overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                  {pool.description || t.noDescription}
                </p>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '12px', fontSize: '13px', color: 'var(--text-muted)' }}>
                  <Users size={14} />
                  <span>{pool.totalMembers || 0} {pool.totalMembers !== 1 ? t.members.plural : t.members.singular}</span>
                </div>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)', borderTop: '1px solid var(--surface-border)', paddingTop: '12px' }}>
                  {t.createdOn} {formatDate(pool.createdAt)}
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
