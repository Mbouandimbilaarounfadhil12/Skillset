import { useState, useEffect } from 'react'
import { ChevronDown, Plus } from 'lucide-react'
import { getTalentPools, addCandidateToPool } from '../../api/TalentPoolApi'
import TalentPoolCreateModal from './TalentPoolCreateModal'
import { useTranslation } from '../../i18n/translations'

export default function AddToPoolButton({ candidateId, onSuccess }) {
  const t = useTranslation().talentPoolButton
  const [pools, setPools] = useState([])
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [toastMessage, setToastMessage] = useState(null)
  const [toastIsError, setToastIsError] = useState(false)

  useEffect(() => {
    loadPools()
  }, [])

  const loadPools = async () => {
    try {
      const data = await getTalentPools()
      setPools(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Error loading pools:', err)
    }
  }

  const handleSelectPool = async (poolId, poolName) => {
    try {
      setLoading(true)
      await addCandidateToPool(poolId, candidateId, '', 'SEARCH_RESULTS')
      setOpen(false)
      setToastIsError(false)
      setToastMessage(`${t.addedToPool} "${poolName}"`)
      if (onSuccess) onSuccess()
      setTimeout(() => setToastMessage(null), 3000)
    } catch (err) {
      console.error('Error adding candidate to pool:', err)
      setToastIsError(true)
      setToastMessage(t.addError)
      setTimeout(() => setToastMessage(null), 3000)
    } finally {
      setLoading(false)
    }
  }

  const handleCreateSuccess = () => {
    setShowCreateModal(false)
    loadPools()
  }

  return (
    <>
      {/* Toast */}
      {toastMessage && (
        <div style={{
          position: 'fixed', bottom: '20px', right: '20px', zIndex: 2000,
          padding: '12px 20px', borderRadius: '6px', fontSize: '13px',
          background: toastIsError ? '#fee' : '#e8f5e9',
          color: toastIsError ? '#c42033' : '#2e7d32',
          border: toastIsError ? '1px solid #fcc' : '1px solid #c8e6c9',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
        }}>
          {toastMessage}
        </div>
      )}

      {/* Create Modal */}
      {showCreateModal && (
        <TalentPoolCreateModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={handleCreateSuccess}
        />
      )}

      {/* Button + Dropdown */}
      <div style={{ position: 'relative', display: 'inline-block' }}>
        <button
          onClick={() => setOpen(!open)}
          disabled={loading}
          style={{
            display: 'flex', alignItems: 'center', gap: '6px',
            padding: '10px 16px', borderRadius: '6px', fontSize: '13px', fontWeight: 600,
            backgroundColor: '#c42033', color: '#fff', border: 'none', cursor: loading ? 'not-allowed' : 'pointer',
            transition: 'all 0.2s', opacity: loading ? 0.7 : 1,
            boxShadow: '0 2px 4px rgba(196,32,51,0.15)'
          }}
          onMouseEnter={(e) => {
            if (!loading) e.target.style.boxShadow = '0 4px 12px rgba(196,32,51,0.25)'
          }}
          onMouseLeave={(e) => {
            e.target.style.boxShadow = '0 2px 4px rgba(196,32,51,0.15)'
          }}
        >
          <Plus size={14} />
          {t.addToPool}
          <ChevronDown size={14} style={{ transition: 'transform 0.2s', transform: open ? 'rotate(180deg)' : 'rotate(0)' }} />
        </button>

        {/* Dropdown Menu */}
        {open && (
          <div style={{
            position: 'absolute', top: '100%', left: 0, marginTop: '8px', zIndex: 1000,
            minWidth: '200px', background: 'var(--surface-card)', borderRadius: '8px',
            border: '1px solid var(--surface-border)', boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            animation: 'slideDown 0.2s ease-out'
          }}>
            <style>{`
              @keyframes slideDown {
                from { transform: translateY(-8px); opacity: 0; }
                to { transform: translateY(0); opacity: 1; }
              }
            `}</style>

            {pools.length === 0 ? (
              <div style={{ padding: '16px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '13px' }}>
                {t.noPoolsAvailable}
              </div>
            ) : (
              <>
                {pools.map(pool => (
                  <button
                    key={pool.id}
                    onClick={() => handleSelectPool(pool.id, pool.name)}
                    disabled={loading}
                    style={{
                      display: 'block', width: '100%', padding: '12px 16px',
                      textAlign: 'left', border: 'none', background: 'none',
                      cursor: loading ? 'not-allowed' : 'pointer', fontSize: '13px',
                      color: 'var(--text-primary)', borderBottom: '1px solid var(--surface-border-soft)', transition: 'background 0.2s'
                    }}
                    onMouseEnter={(e) => {
                      if (!loading) e.target.style.background = 'var(--surface-border-soft)'
                    }}
                    onMouseLeave={(e) => {
                      e.target.style.background = 'none'
                    }}
                  >
                    <div style={{ fontWeight: 500, marginBottom: '2px' }}>{pool.name}</div>
                    <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                      {pool.totalMembers || 0} {pool.totalMembers !== 1 ? t.members.plural : t.members.singular}
                    </div>
                  </button>
                ))}
                <div style={{ height: '1px', background: 'var(--surface-border-soft)' }} />
              </>
            )}

            {/* Create New Pool Option */}
            <button
              onClick={() => {
                setOpen(false)
                setShowCreateModal(true)
              }}
              style={{
                display: 'block', width: '100%', padding: '12px 16px',
                textAlign: 'left', border: 'none', background: 'none',
                cursor: 'pointer', fontSize: '13px', color: '#c42033',
                fontWeight: 500, transition: 'background 0.2s',
                borderBottomLeftRadius: '8px', borderBottomRightRadius: '8px'
              }}
              onMouseEnter={(e) => e.target.style.background = 'var(--surface-page-alt)'}
              onMouseLeave={(e) => e.target.style.background = 'none'}
            >
              <Plus size={14} style={{ display: 'inline', marginRight: '8px', verticalAlign: 'middle' }} />
              {t.createNewPool}
            </button>
          </div>
        )}
      </div>

      {/* Click outside to close */}
      {open && (
        <div
          onClick={() => setOpen(false)}
          style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, zIndex: 999
          }}
        />
      )}
    </>
  )
}
