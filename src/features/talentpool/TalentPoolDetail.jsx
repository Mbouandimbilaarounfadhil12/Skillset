import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Trash2, ChevronLeft, ChevronRight, Sparkles } from 'lucide-react'
import {
  getPoolMembers, removeCandidateFromPool, updateMemberStatus, getRecommendedCandidates
} from '../../api/TalentPoolApi'
import AppNavbar from '../../components/common/AppNavbar'
import { useTranslation } from '../../i18n/translations'

const STATUS_COLORS = {
  ACTIVE: '#4caf50',
  CONTACTED: '#ff9800',
  HIRED: '#2196f3',
  REJECTED: '#f44336'
}

export default function TalentPoolDetail() {
  const { poolId } = useParams()
  const navigate = useNavigate()
  const t = useTranslation().talentPoolDetail

  const [pool, setPool] = useState(null)
  const [members, setMembers] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [showRecommendations, setShowRecommendations] = useState(false)
  const [recommendedCandidates, setRecommendedCandidates] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    loadPoolData()
  }, [poolId])

  useEffect(() => {
    loadMembers()
  }, [poolId, page])

  const loadPoolData = async () => {
    try {
      setLoading(true)
      setError(null)
      // TODO: fetch pool data from API
      setPool({ id: poolId, name: 'Vivier Développeurs Douala', description: 'Candidats tech présélectionnés pour vos prochains postes.' })
    } catch (err) {
      console.error('Error loading pool:', err)
      setError(t.loadError)
    } finally {
      setLoading(false)
    }
  }

  const loadMembers = async () => {
    try {
      const data = await getPoolMembers(poolId, page, 20)
      setMembers(Array.isArray(data?.content) ? data.content : [])
      setTotalPages(data?.totalPages || 1)
    } catch (err) {
      console.error('Error loading members:', err)
    }
  }

  const handleRemoveMember = async (candidateId) => {
    try {
      await removeCandidateFromPool(poolId, candidateId)
      loadMembers()
    } catch (err) {
      console.error('Error removing member:', err)
    }
  }

  const handleStatusChange = async (candidateId, newStatus) => {
    try {
      await updateMemberStatus(poolId, candidateId, newStatus)
      loadMembers()
    } catch (err) {
      console.error('Error updating status:', err)
    }
  }

  const loadRecommendations = async () => {
    try {
      const data = await getRecommendedCandidates(poolId)
      setRecommendedCandidates(Array.isArray(data) ? data : [])
      setShowRecommendations(true)
    } catch (err) {
      console.error('Error loading recommendations:', err)
    }
  }

  const countByStatus = (status) => {
    return members.filter(m => m.status === status).length
  }

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', background: 'var(--surface-page)' }}>
        <AppNavbar />
        <div style={{ textAlign: 'center', padding: '60px 20px' }}>
          <div style={{
            display: 'inline-block', width: '32px', height: '32px',
            border: '3px solid var(--surface-border)', borderTop: '3px solid #c42033', borderRadius: '50%',
            animation: 'spin 0.8s linear infinite'
          }} />
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
      </div>
    )
  }

  if (error || !pool) {
    return (
      <div style={{ minHeight: '100vh', background: 'var(--surface-page)' }}>
        <AppNavbar />
        <main style={{ maxWidth: '1200px', margin: '0 auto', padding: '40px 20px' }}>
          <button
            onClick={() => navigate('/employer/talent-pools')}
            style={{ padding: '8px 16px', background: 'var(--surface-border-soft)', border: 'none', borderRadius: '6px', cursor: 'pointer', marginBottom: '20px', color: 'var(--text-primary)' }}
          >
            <ChevronLeft size={16} style={{ display: 'inline', marginRight: '8px' }} />
            {t.back}
          </button>
          <div style={{ padding: '20px', borderRadius: '8px', backgroundColor: '#fee', border: '1px solid #fcc', color: '#c42033' }}>
            {error || t.notFound}
          </div>
        </main>
      </div>
    )
  }

  return (
    <div style={{ minHeight: '100vh', background: 'var(--surface-page)' }}>
      <AppNavbar />

      <main style={{ maxWidth: '1200px', margin: '0 auto', padding: '40px 20px' }}>
        {/* Back Button */}
        <button
          onClick={() => navigate('/employer/talent-pools')}
          style={{
            display: 'flex', alignItems: 'center', gap: '8px',
            padding: '8px 16px', background: 'transparent', border: '1px solid var(--surface-border)', borderRadius: '6px',
            cursor: 'pointer', color: 'var(--text-secondary)', fontSize: '14px', marginBottom: '24px'
          }}
        >
          <ChevronLeft size={16} /> {t.backToPools}
        </button>

        {/* Header */}
        <div style={{ marginBottom: '40px' }}>
          <h1 style={{ fontSize: '28px', fontWeight: 700, color: 'var(--text-primary)', margin: '0 0 8px 0' }}>
            {pool.name}
          </h1>
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)', margin: 0 }}>
            {pool.description}
          </p>
        </div>

        {/* Stats */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '40px' }}>
          {[
            { label: t.total, value: members.length, color: 'var(--text-secondary)' },
            { label: t.active, value: countByStatus('ACTIVE'), color: '#4caf50' },
            { label: t.contacted, value: countByStatus('CONTACTED'), color: '#ff9800' },
            { label: t.hired, value: countByStatus('HIRED'), color: '#2196f3' }
          ].map((stat, i) => (
            <div key={i} style={{
              padding: '20px', borderRadius: '8px', background: 'var(--surface-card)', border: '1px solid var(--surface-border)'
            }}>
              <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: '0 0 8px 0' }}>{stat.label}</p>
              <p style={{ fontSize: '24px', fontWeight: 700, color: stat.color, margin: 0 }}>
                {stat.value}
              </p>
            </div>
          ))}
        </div>

        {/* Members Table */}
        <div style={{ background: 'var(--surface-card)', borderRadius: '8px', border: '1px solid var(--surface-border)', marginBottom: '24px', overflow: 'hidden' }}>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--surface-border)', background: 'var(--surface-page)' }}>
                  <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: 'var(--text-secondary)' }}>{t.name}</th>
                  <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: 'var(--text-secondary)' }}>{t.title}</th>
                  <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: 'var(--text-secondary)' }}>{t.score}</th>
                  <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: 'var(--text-secondary)' }}>{t.notes}</th>
                  <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: 'var(--text-secondary)' }}>{t.status}</th>
                  <th style={{ padding: '12px 16px', textAlign: 'center', fontWeight: 600, color: 'var(--text-secondary)' }}>{t.actions}</th>
                </tr>
              </thead>
              <tbody>
                {members.length === 0 ? (
                  <tr>
                    <td colSpan="6" style={{ padding: '40px 16px', textAlign: 'center', color: 'var(--text-muted)' }}>
                      {t.noCandidates}
                    </td>
                  </tr>
                ) : (
                  members.map(member => (
                    <tr key={member.id} style={{ borderBottom: '1px solid var(--surface-border)' }}>
                      <td style={{ padding: '12px 16px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          {member.avatar && (
                            <img src={member.avatar} alt={member.name} style={{
                              width: '32px', height: '32px', borderRadius: '50%', background: 'var(--surface-border-soft)'
                            }} />
                          )}
                          <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{member.name}</span>
                        </div>
                      </td>
                      <td style={{ padding: '12px 16px', color: 'var(--text-secondary)' }}>{member.title || '—'}</td>
                      <td style={{ padding: '12px 16px', color: 'var(--text-secondary)' }}>{member.score ? `${member.score}%` : '—'}</td>
                      <td style={{ padding: '12px 16px', color: 'var(--text-secondary)', maxWidth: '150px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {member.notes || '—'}
                      </td>
                      <td style={{ padding: '12px 16px' }}>
                        <select
                          value={member.status || 'ACTIVE'}
                          onChange={(e) => handleStatusChange(member.id, e.target.value)}
                          style={{
                            padding: '6px 8px', borderRadius: '4px', border: `1.5px solid ${STATUS_COLORS[member.status] || '#ccc'}`,
                            background: 'var(--surface-card)', color: STATUS_COLORS[member.status] || 'var(--text-secondary)',
                            fontSize: '12px', fontWeight: 500, cursor: 'pointer'
                          }}
                        >
                          <option value="ACTIVE">{t.statusActive}</option>
                          <option value="CONTACTED">{t.statusContacted}</option>
                          <option value="HIRED">{t.statusHired}</option>
                          <option value="REJECTED">{t.statusRejected}</option>
                        </select>
                      </td>
                      <td style={{ padding: '12px 16px', textAlign: 'center' }}>
                        <button
                          onClick={() => handleRemoveMember(member.id)}
                          style={{
                            padding: '6px 8px', borderRadius: '4px', border: '1px solid #fee',
                            background: 'var(--surface-card)', color: '#c42033', cursor: 'pointer', fontSize: '12px'
                          }}
                          title={t.remove}
                        >
                          <Trash2 size={14} />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '40px' }}>
            <button
              onClick={() => setPage(Math.max(0, page - 1))}
              disabled={page === 0}
              style={{
                padding: '8px 16px', borderRadius: '6px', border: '1px solid var(--surface-border)', background: 'var(--surface-card)',
                cursor: page === 0 ? 'not-allowed' : 'pointer', opacity: page === 0 ? 0.5 : 1,
                display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-primary)'
              }}
            >
              <ChevronLeft size={14} /> {t.previous}
            </button>
            <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
              {t.pageOf.replace('{p}', page + 1).replace('{t}', totalPages)}
            </span>
            <button
              onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
              disabled={page >= totalPages - 1}
              style={{
                padding: '8px 16px', borderRadius: '6px', border: '1px solid var(--surface-border)', background: 'var(--surface-card)',
                cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer', opacity: page >= totalPages - 1 ? 0.5 : 1,
                display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-primary)'
              }}
            >
              {t.next} <ChevronRight size={14} />
            </button>
          </div>
        )}

        {/* Recommendations Button */}
        <button
          onClick={loadRecommendations}
          style={{
            display: 'flex', alignItems: 'center', gap: '8px',
            padding: '12px 24px', borderRadius: '8px', fontSize: '14px', fontWeight: 600,
            backgroundColor: 'var(--surface-card)', color: '#c42033', border: '1.5px solid #c42033',
            cursor: 'pointer', transition: 'all 0.2s'
          }}
          onMouseEnter={(e) => {
            e.target.style.backgroundColor = 'var(--surface-page-alt)'
          }}
          onMouseLeave={(e) => {
            e.target.style.backgroundColor = 'var(--surface-card)'
          }}
        >
          <Sparkles size={16} /> {t.viewRecommendations}
        </button>

        {/* Recommendations Section */}
        {showRecommendations && (
          <div style={{ marginTop: '40px', padding: '20px', borderRadius: '8px', background: 'var(--surface-card)', border: '1px solid var(--surface-border)' }}>
            <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-primary)', margin: '0 0 16px 0' }}>
              {t.recommendedCandidates}
            </h3>
            {recommendedCandidates.length === 0 ? (
              <p style={{ fontSize: '13px', color: 'var(--text-muted)', margin: 0 }}>{t.noRecommendations}</p>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: '16px' }}>
                {recommendedCandidates.map(candidate => (
                  <div key={candidate.id} style={{
                    padding: '16px', borderRadius: '8px', background: 'var(--surface-border-soft)', textAlign: 'center'
                  }}>
                    {candidate.avatar && (
                      <img src={candidate.avatar} alt={candidate.name} style={{
                        width: '48px', height: '48px', borderRadius: '50%', marginBottom: '8px', background: 'var(--surface-border)'
                      }} />
                    )}
                    <p style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)', margin: '0 0 4px 0' }}>
                      {candidate.name}
                    </p>
                    <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: '0 0 8px 0' }}>
                      {candidate.score ? `${t.score}: ${candidate.score}%` : 'N/A'}
                    </p>
                    <button
                      style={{
                        padding: '6px 12px', borderRadius: '4px', fontSize: '12px', fontWeight: 600,
                        backgroundColor: '#c42033', color: '#fff', border: 'none', cursor: 'pointer'
                      }}
                    >
                      {t.add}
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  )
}
