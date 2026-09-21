import { useState, useMemo } from 'react'
import { Upload, Trash2, ExternalLink, Loader2, Check, AlertCircle, Globe } from 'lucide-react'
import { publishToJobBoards, unpublishFromJobBoards } from '../../api/IntegrationsApi'

/**
 * Composant pour la publication multi-régionale d'offres d'emploi.
 * Permet de sélectionner les pays cibles et affiche l'aperçu des job boards
 * qui seront appelés selon le routage configuré.
 */
export default function PublishToMultipleJobBoardsButton({
  jobId,
  isPublished: initialPublished,
  publications: initialPublications = [],
  onSuccess
}) {
  // État principal
  const [selectedCountries, setSelectedCountries] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [published, setPublished] = useState(initialPublished ?? false)
  const [toastMessage, setToastMessage] = useState('')
  const [publishResults, setPublishResults] = useState([])
  const [showResults, setShowResults] = useState(false)

  // Mapping pays → drapeaux et noms
  const countryOptions = [
    { code: 'FR', name: 'France', flag: '🇫🇷' },
    { code: 'CM', name: 'Cameroon', flag: '🇨🇲' },
    { code: 'SN', name: 'Senegal', flag: '🇸🇳' },
    { code: 'CI', name: "Côte d'Ivoire", flag: '🇨🇮' },
    { code: 'NG', name: 'Nigeria', flag: '🇳🇬' },
    { code: 'KE', name: 'Kenya', flag: '🇰🇪' },
  ]

  // Mapping partenaire → job boards cibles (selon JobBoardConfig)
  const countryToJobBoards = {
    'FR': ['France Travail', 'LinkedIn'],
    'CM': ['BrighterMonday', 'LinkedIn', 'Jobartisan'],
    'SN': ['BrighterMonday', 'LinkedIn'],
    'CI': ['BrighterMonday', 'LinkedIn'],
    'NG': ['BrighterMonday', 'LinkedIn'],
    'KE': ['BrighterMonday', 'LinkedIn'],
  }

  // Calcule les job boards pour les pays sélectionnés
  const targetJobBoards = useMemo(() => {
    const boards = new Set()
    selectedCountries.forEach(code => {
      countryToJobBoards[code]?.forEach(board => boards.add(board))
    })
    return Array.from(boards).sort()
  }, [selectedCountries])

  const showToast = (msg, isError = false) => {
    setToastMessage(msg)
    setError(isError ? msg : '')
    setTimeout(() => {
      setToastMessage('')
      setError('')
    }, 4000)
  }

  const handleCountryToggle = (code) => {
    setSelectedCountries(prev =>
      prev.includes(code)
        ? prev.filter(c => c !== code)
        : [...prev, code]
    )
  }

  const handlePublish = async () => {
    if (selectedCountries.length === 0) {
      showToast('Veuillez sélectionner au moins un pays', true)
      return
    }

    try {
      setLoading(true)
      setError('')
      const response = await publishToJobBoards(jobId, selectedCountries)

      setPublishResults(response.results || [])
      setPublished(true)
      setShowResults(true)

      const successCount = response.successCount ?? 0
      const failureCount = response.failureCount ?? 0

      if (failureCount === 0) {
        showToast(`✓ Offre publiée sur ${successCount} job board(s)`)
      } else {
        showToast(`⚠ Publiée sur ${successCount} board(s), ${failureCount} erreur(s)`, true)
      }

      if (onSuccess) onSuccess()
    } catch (err) {
      console.error('Error publishing to job boards:', err)
      const errorMsg = err.response?.data?.error || 'Erreur lors de la publication'
      showToast(errorMsg, true)
    } finally {
      setLoading(false)
    }
  }

  const handleUnpublish = async () => {
    if (!window.confirm('Êtes-vous sûr de vouloir retirer cette offre de tous les job boards ?')) return

    try {
      setLoading(true)
      setError('')
      await unpublishFromJobBoards(jobId)

      setPublished(false)
      setSelectedCountries([])
      setPublishResults([])
      setShowResults(false)

      showToast('Offre retirée de tous les job boards')
      if (onSuccess) onSuccess()
    } catch (err) {
      console.error('Error unpublishing from job boards:', err)
      const errorMsg = err.response?.data?.error || 'Erreur lors du retrait'
      showToast(errorMsg, true)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      {/* Toast */}
      {toastMessage && (
        <div
          style={{
            position: 'fixed',
            bottom: '20px',
            right: '20px',
            zIndex: 2000,
            padding: '12px 20px',
            borderRadius: '6px',
            fontSize: '13px',
            background: error ? '#fee' : '#e8f5e9',
            color: error ? '#c42033' : '#2e7d32',
            border: error ? '1px solid #fcc' : '1px solid #c8e6c9',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            animation: 'slideIn 0.3s ease-out',
          }}
        >
          {error ? '⚠️' : '✓'} {toastMessage}
        </div>
      )}

      {loading ? (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '10px 16px',
            borderRadius: '6px',
            fontSize: '13px',
            color: '#999',
            background: '#f9f9f9',
            border: '1px solid #eee',
          }}
        >
          <Loader2 size={14} style={{ animation: 'spin 1s linear infinite' }} />
          Traitement...
        </div>
      ) : published && showResults ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {/* Résultats de publication */}
          <div style={{ padding: '12px', background: '#f9f9f9', borderRadius: '6px', borderLeft: '3px solid #2e7d32' }}>
            <div style={{ fontWeight: 600, marginBottom: '8px', fontSize: '13px', color: '#333' }}>
              ✓ Publication complétée
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {publishResults.map(result => (
                <div
                  key={result.partner}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '8px',
                    background: result.status === 'PUBLISHED' ? '#e8f5e9' : '#fee',
                    border: result.status === 'PUBLISHED' ? '1px solid #c8e6c9' : '1px solid #fcc',
                    borderRadius: '4px',
                    fontSize: '12px',
                  }}
                >
                  {result.status === 'PUBLISHED' ? (
                    <Check size={12} style={{ color: '#2e7d32' }} />
                  ) : (
                    <AlertCircle size={12} style={{ color: '#c42033' }} />
                  )}
                  <span style={{ flex: 1 }}>{result.partner}</span>
                  {result.externalUrl && (
                    <a
                      href={result.externalUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{
                        color: '#0066cc',
                        textDecoration: 'none',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '4px',
                      }}
                    >
                      Voir
                      <ExternalLink size={10} />
                    </a>
                  )}
                  {result.errorMessage && (
                    <span style={{ color: '#c42033', fontSize: '11px' }}>
                      {result.errorMessage}
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>

          <button
            onClick={handleUnpublish}
            disabled={loading}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              padding: '10px 16px',
              borderRadius: '6px',
              fontSize: '13px',
              fontWeight: 600,
              background: '#fff',
              color: '#c42033',
              border: '1px solid #fcc',
              cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              opacity: loading ? 0.7 : 1,
            }}
            onMouseEnter={(e) => {
              if (!loading) {
                e.target.style.background = '#fee'
                e.target.style.borderColor = '#fbb'
              }
            }}
            onMouseLeave={(e) => {
              e.target.style.background = '#fff'
              e.target.style.borderColor = '#fcc'
            }}
          >
            <Trash2 size={14} />
            Retirer de tous les job boards
          </button>
        </div>
      ) : published ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '10px 16px',
              borderRadius: '6px',
              fontSize: '13px',
              color: '#2e7d32',
              background: '#e8f5e9',
              border: '1px solid #c8e6c9',
              fontWeight: 600,
            }}
          >
            <Check size={14} />
            Offre publiée
          </div>

          <button
            onClick={handleUnpublish}
            disabled={loading}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              padding: '10px 16px',
              borderRadius: '6px',
              fontSize: '13px',
              fontWeight: 600,
              background: '#fff',
              color: '#c42033',
              border: '1px solid #fcc',
              cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              opacity: loading ? 0.7 : 1,
            }}
            onMouseEnter={(e) => {
              if (!loading) {
                e.target.style.background = '#fee'
                e.target.style.borderColor = '#fbb'
              }
            }}
            onMouseLeave={(e) => {
              e.target.style.background = '#fff'
              e.target.style.borderColor = '#fcc'
            }}
          >
            <Trash2 size={14} />
            Retirer
          </button>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {/* Sélecteur de pays */}
          <div>
            <label style={{ display: 'block', marginBottom: '8px', fontSize: '12px', fontWeight: 600, color: '#333' }}>
              <Globe size={14} style={{ marginRight: '4px', display: 'inline' }} />
              Pays cibles
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px' }}>
              {countryOptions.map(country => (
                <button
                  key={country.code}
                  onClick={() => handleCountryToggle(country.code)}
                  style={{
                    padding: '8px 12px',
                    borderRadius: '4px',
                    fontSize: '12px',
                    border: selectedCountries.includes(country.code)
                      ? '2px solid #c42033'
                      : '1px solid #ddd',
                    background: selectedCountries.includes(country.code)
                      ? '#fef5f6'
                      : '#fff',
                    color: selectedCountries.includes(country.code)
                      ? '#c42033'
                      : '#666',
                    cursor: 'pointer',
                    transition: 'all 0.2s ease',
                    fontWeight: selectedCountries.includes(country.code) ? 600 : 400,
                  }}
                  onMouseEnter={(e) => {
                    e.target.style.borderColor = '#c42033'
                  }}
                  onMouseLeave={(e) => {
                    e.target.style.borderColor = selectedCountries.includes(country.code)
                      ? '#c42033'
                      : '#ddd'
                  }}
                >
                  {country.flag} {country.code}
                </button>
              ))}
            </div>
          </div>

          {/* Aperçu des job boards */}
          {targetJobBoards.length > 0 && (
            <div
              style={{
                padding: '10px 12px',
                background: '#f0f7ff',
                border: '1px solid #b3d9ff',
                borderRadius: '4px',
                fontSize: '12px',
              }}
            >
              <div style={{ color: '#0066cc', fontWeight: 600, marginBottom: '6px' }}>
                Job boards cibles ({targetJobBoards.length}) :
              </div>
              <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                {targetJobBoards.map(board => (
                  <span
                    key={board}
                    style={{
                      padding: '4px 8px',
                      background: '#e6f2ff',
                      borderRadius: '3px',
                      color: '#0066cc',
                      fontSize: '11px',
                      fontWeight: 500,
                    }}
                  >
                    {board}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Bouton Publier */}
          <button
            onClick={handlePublish}
            disabled={loading || selectedCountries.length === 0}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              padding: '10px 16px',
              borderRadius: '6px',
              fontSize: '13px',
              fontWeight: 600,
              background: selectedCountries.length === 0 ? '#ccc' : '#c42033',
              color: '#fff',
              border: 'none',
              cursor: selectedCountries.length === 0 || loading ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              opacity: selectedCountries.length === 0 || loading ? 0.7 : 1,
              boxShadow: selectedCountries.length === 0 ? 'none' : '0 2px 4px rgba(196,32,51,0.15)',
            }}
            onMouseEnter={(e) => {
              if (selectedCountries.length > 0 && !loading) {
                e.target.style.background = '#a81729'
                e.target.style.boxShadow = '0 4px 12px rgba(196,32,51,0.25)'
              }
            }}
            onMouseLeave={(e) => {
              e.target.style.background = selectedCountries.length === 0 ? '#ccc' : '#c42033'
              e.target.style.boxShadow = selectedCountries.length === 0 ? 'none' : '0 2px 4px rgba(196,32,51,0.15)'
            }}
          >
            <Upload size={14} />
            Publier sur {selectedCountries.length} pays
          </button>
        </div>
      )}

      <style>{`
        @keyframes slideIn {
          from {
            transform: translateX(400px);
            opacity: 0;
          }
          to {
            transform: translateX(0);
            opacity: 1;
          }
        }
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  )
}
