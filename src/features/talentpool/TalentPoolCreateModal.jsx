import { useState } from 'react'
import { X } from 'lucide-react'
import { createTalentPool } from '../../api/TalentPoolApi'
import { useTranslation } from '../../i18n/translations'

export default function TalentPoolCreateModal({ onClose, onSuccess }) {
  const t = useTranslation().talentPool
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [jobListingId, setJobListingId] = useState('')
  const [isPublic, setIsPublic] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)

    if (!name.trim()) {
      setError(t.nameRequired)
      return
    }

    try {
      setLoading(true)
      const dto = {
        name: name.trim(),
        description: description.trim() || null,
        jobListingId: jobListingId || null,
        isPublic
      }
      await createTalentPool(dto)
      onSuccess()
    } catch (err) {
      console.error('Error creating pool:', err)
      setError(err.response?.data?.message || t.createError)
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = () => {
    onClose()
  }

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      background: 'rgba(0, 0, 0, 0.5)', display: 'flex', alignItems: 'center',
      justifyContent: 'center', zIndex: 1000
    }}>
      <div style={{
        background: 'var(--surface-card)', borderRadius: '12px', padding: '32px',
        maxWidth: '500px', width: '90vw', boxShadow: '0 10px 40px rgba(0, 0, 0, 0.2)',
        animation: 'slideUp 0.3s ease-out'
      }}>
        <style>{`@keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }`}</style>

        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
            {t.modalTitle}
          </h2>
          <button
            onClick={handleCancel}
            style={{
              background: 'none', border: 'none', cursor: 'pointer', padding: '4px',
              color: 'var(--text-muted)', transition: 'color 0.2s'
            }}
            onMouseEnter={(e) => e.target.style.color = 'var(--text-primary)'}
            onMouseLeave={(e) => e.target.style.color = 'var(--text-muted)'}
          >
            <X size={20} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit}>
          {/* Name */}
          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '8px' }}>
              {t.poolName} <span style={{ color: '#c42033' }}>*</span>
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={t.poolNamePlaceholder}
              style={{
                width: '100%', padding: '10px 12px', borderRadius: '6px',
                border: '1px solid var(--surface-border)', fontSize: '13px',
                background: 'var(--surface-card)', color: 'var(--text-primary)',
                boxSizing: 'border-box', transition: 'border-color 0.2s'
              }}
              onFocus={(e) => e.target.style.borderColor = '#c42033'}
              onBlur={(e) => e.target.style.borderColor = 'var(--surface-border)'}
            />
          </div>

          {/* Description */}
          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '8px' }}>
              {t.description}
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t.descriptionPlaceholder}
              style={{
                width: '100%', padding: '10px 12px', borderRadius: '6px',
                border: '1px solid var(--surface-border)', fontSize: '13px', minHeight: '100px',
                background: 'var(--surface-card)', color: 'var(--text-primary)',
                fontFamily: 'inherit', boxSizing: 'border-box', transition: 'border-color 0.2s',
                resize: 'vertical'
              }}
              onFocus={(e) => e.target.style.borderColor = '#c42033'}
              onBlur={(e) => e.target.style.borderColor = 'var(--surface-border)'}
            />
          </div>

          {/* Job Listing */}
          <div style={{ marginBottom: '20px' }}>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '8px' }}>
              {t.linkedJob}
            </label>
            <select
              value={jobListingId}
              onChange={(e) => setJobListingId(e.target.value)}
              style={{
                width: '100%', padding: '10px 12px', borderRadius: '6px',
                border: '1px solid var(--surface-border)', fontSize: '13px',
                background: 'var(--surface-card)', color: 'var(--text-primary)',
                boxSizing: 'border-box', transition: 'border-color 0.2s'
              }}
              onFocus={(e) => e.target.style.borderColor = '#c42033'}
              onBlur={(e) => e.target.style.borderColor = 'var(--surface-border)'}
            >
              <option value="">{t.selectJob}</option>
              {/* TODO: populate with actual jobs from API */}
            </select>
          </div>

          {/* Public Checkbox */}
          <div style={{ marginBottom: '24px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <input
              type="checkbox"
              id="isPublic"
              checked={isPublic}
              onChange={(e) => setIsPublic(e.target.checked)}
              style={{ cursor: 'pointer' }}
            />
            <label htmlFor="isPublic" style={{ fontSize: '13px', color: 'var(--text-secondary)', cursor: 'pointer', margin: 0 }}>
              {t.makePublic}
            </label>
          </div>

          {/* Error */}
          {error && (
            <div style={{
              padding: '12px', borderRadius: '6px', backgroundColor: '#fee',
              border: '1px solid #fcc', color: '#c42033', fontSize: '13px',
              marginBottom: '20px'
            }}>
              {error}
            </div>
          )}

          {/* Buttons */}
          <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
            <button
              type="button"
              onClick={handleCancel}
              disabled={loading}
              style={{
                padding: '10px 24px', borderRadius: '6px', border: '1px solid var(--surface-border)',
                background: 'var(--surface-card)', fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)',
                cursor: loading ? 'not-allowed' : 'pointer', opacity: loading ? 0.5 : 1,
                transition: 'all 0.2s'
              }}
              onMouseEnter={(e) => {
                if (!loading) e.target.style.backgroundColor = 'var(--surface-border-soft)'
              }}
              onMouseLeave={(e) => {
                e.target.style.backgroundColor = 'var(--surface-card)'
              }}
            >
              {t.cancel}
            </button>
            <button
              type="submit"
              disabled={loading}
              style={{
                padding: '10px 24px', borderRadius: '6px', border: 'none',
                background: '#c42033', color: '#fff', fontSize: '13px', fontWeight: 600,
                cursor: loading ? 'not-allowed' : 'pointer', opacity: loading ? 0.6 : 1,
                transition: 'all 0.2s', display: 'flex', alignItems: 'center', gap: '8px'
              }}
              onMouseEnter={(e) => {
                if (!loading) e.target.style.boxShadow = '0 4px 12px rgba(196,32,51,0.3)'
              }}
              onMouseLeave={(e) => {
                e.target.style.boxShadow = 'none'
              }}
            >
              {loading ? (
                <>
                  <div style={{
                    display: 'inline-block', width: '14px', height: '14px',
                    border: '2px solid #fff', borderTop: '2px solid transparent', borderRadius: '50%',
                    animation: 'spin 0.6s linear infinite'
                  }} />
                  {t.creating}
                </>
              ) : (
                t.create
              )}
            </button>
          </div>
        </form>

        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      </div>
    </div>
  )
}
