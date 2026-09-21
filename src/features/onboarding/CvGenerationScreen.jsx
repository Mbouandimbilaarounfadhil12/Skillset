import { useEffect, useState } from 'react'
import { Check, Download, Eye } from 'lucide-react'
import { useTranslation } from '../../i18n/translations'
import './CvGenerationScreen.css'

const STEP_IDS = ['analysis', 'content', 'layout', 'upload', 'email']
const STEP_DURATIONS = { analysis: 2000, content: 2500, layout: 2000, upload: 1500, email: 1000 }

/**
 * Écran de génération du CV.
 * Phase 1 : Animation d'étapes (loading)
 * Phase 2 : Écran succès avec aperçu CV + actions
 */
export default function CvGenerationScreen({ cvUrl, country, onDownload, onClose }) {
  const t = useTranslation().onboarding.cvGeneration
  const [completedSteps, setCompletedSteps] = useState([])
  const [currentStep, setCurrentStep] = useState(0)
  const [isGenerating, setIsGenerating] = useState(true)

  useEffect(() => {
    if (currentStep >= STEP_IDS.length) {
      setIsGenerating(false)
      return
    }

    const stepId = STEP_IDS[currentStep]
    const timer = setTimeout(() => {
      setCompletedSteps(prev => [...prev, stepId])
      setCurrentStep(prev => prev + 1)
    }, STEP_DURATIONS[stepId])

    return () => clearTimeout(timer)
  }, [currentStep])

  if (isGenerating) {
    return (
      <div className="cgs-generating">
        <div className="cgs-generating-card">
          <div className="cgs-generating-header">
            <h2 className="cgs-generating-title">{t.generatingTitle}</h2>
            <p className="cgs-generating-subtitle">{t.generatingSubtitle}</p>
          </div>

          <div className="cgs-steps">
            {STEP_IDS.map((stepId, idx) => {
              const isCompleted = completedSteps.includes(stepId)
              const isCurrent = idx === currentStep && !isCompleted

              return (
                <div
                  key={stepId}
                  className={`cgs-step ${isCompleted ? 'cgs-step--completed' : ''} ${isCurrent ? 'cgs-step--current' : ''}`}
                >
                  <div className="cgs-step-icon">
                    {isCompleted ? (
                      <Check size={20} className="cgs-step-check" />
                    ) : isCurrent ? (
                      <div className="cgs-step-spinner" />
                    ) : (
                      <div className="cgs-step-dot" />
                    )}
                  </div>
                  <span className="cgs-step-label">{t.steps[stepId]}</span>
                </div>
              )
            })}
          </div>

          <div className="cgs-progress-bar">
            <div
              className="cgs-progress-fill"
              style={{
                width: `${(completedSteps.length / STEP_IDS.length) * 100}%`,
              }}
            />
          </div>
        </div>
      </div>
    )
  }

  // SUCCESS SCREEN
  return (
    <div className="cgs-success">
      <div className="cgs-success-card">
        <div className="cgs-success-header">
          <div className="cgs-success-icon">
            <Check size={32} />
          </div>
          <h2 className="cgs-success-title">{t.successTitle}</h2>
          <p className="cgs-success-subtitle">{t.successSubtitle}</p>
        </div>

        <div className="cgs-success-body">
          <div className="cgs-cv-preview">
            <div className="cgs-cv-thumbnail">
              <div className="cgs-cv-placeholder">
                <span>📄</span>
                <span>CV</span>
              </div>
            </div>
            <div className="cgs-cv-info">
              <p className="cgs-cv-format">{t.formatLabel}</p>
              <p className="cgs-cv-country">{t.adaptedFor} {country || t.yourCountry}</p>
              <p className="cgs-cv-ats">{t.atsOptimized}</p>
            </div>
          </div>

          <div className="cgs-success-messages">
            <div className="cgs-message cgs-message--email">
              <span className="cgs-message-icon">✉️</span>
              <div className="cgs-message-text">
                <p className="cgs-message-title">{t.emailSentTitle}</p>
                <p className="cgs-message-desc">{t.emailSentDesc}</p>
              </div>
            </div>

            <div className="cgs-message cgs-message--offers">
              <span className="cgs-message-icon">🎯</span>
              <div className="cgs-message-text">
                <p className="cgs-message-title">{t.personalizedOffersTitle}</p>
                <p className="cgs-message-desc">{t.personalizedOffersDesc} {country || t.you}</p>
              </div>
            </div>
          </div>
        </div>

        <div className="cgs-success-actions">
          {cvUrl && (
            <a
              href={cvUrl}
              download
              className="cgs-btn cgs-btn--primary"
              onClick={onDownload}
            >
              <Download size={16} />
              {t.downloadCv}
            </a>
          )}

          <button className="cgs-btn cgs-btn--secondary" onClick={onClose}>
            <Eye size={16} />
            {t.viewProfile}
          </button>
        </div>

        <div className="cgs-success-footer">
          <p className="cgs-footer-text">
            {t.footerTip}
          </p>
        </div>
      </div>
    </div>
  )
}
