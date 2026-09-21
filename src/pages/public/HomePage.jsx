import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Sparkles, Briefcase, Users, Clock, TrendingUp,
  Target, FileText, ShieldCheck, KeyRound, Flag,
  Globe, Plus, Minus, ArrowRight, Bot, Rocket,
  CheckCircle2,
} from 'lucide-react'
import PublicNavbar from '../../components/common/PublicNavbar'
import Footer from '../../components/common/Footer'
import { useTranslation } from '../../i18n/translations'
import './HomePage.css'

const STAT_ICONS = [Users, Briefcase, Clock, TrendingUp]

function FaqItem({ q, a, isOpen, onToggle }) {
  return (
    <div className="lp-faq-item">
      <button className="lp-faq-question" type="button" onClick={onToggle}>
        <span>{q}</span>
        {isOpen ? <Minus size={18} /> : <Plus size={18} />}
      </button>
      {isOpen && <p className="lp-faq-answer">{a}</p>}
    </div>
  )
}

export default function HomePage() {
  const [openFaq, setOpenFaq] = useState(0)
  const t = useTranslation().home

  const [heroLine1, heroPrefix, heroAccent, heroSuffix] = t.heroTitle
  const [f1Prefix, f1Accent] = t.feature1.title
  const [f2Prefix, f2Accent] = t.feature2.title
  const [regionsPrefix, regionsAccent] = t.regionsTitle

  return (
    <div className="lp-shell">
      <PublicNavbar />

      {/* ── Hero ── */}
      <main className="lp-hero">
        <span className="lp-badge">
          <span className="lp-badge-dot" /> {t.badge}
        </span>

        <h1 className="lp-hero-title">
          {heroLine1}<br />
          {heroPrefix}<span className="lp-accent">{heroAccent}</span>{heroSuffix}
        </h1>

        <p className="lp-hero-sub">{t.heroSub}</p>

        <div className="lp-hero-actions">
          <Link to="/register" className="lp-hero-cta">Create account</Link>
          <Link to="/login" className="lp-hero-cta lp-hero-cta--secondary">View demo</Link>
        </div>

        <div className="lp-trust-row" aria-label="Trust indicators">
          <span><CheckCircle2 size={14} /> 240+ hiring teams</span>
          <span><Bot size={14} /> AI-assisted screening</span>
          <span><Rocket size={14} /> 3x faster shortlist</span>
        </div>

        <section className="lp-results">
          <p className="lp-results-heading">
            <Sparkles size={13} /> {t.resultsHeading}
          </p>

          <div className="lp-stats-grid">
            {t.stats.map(({ value, label }, i) => {
              const Icon = STAT_ICONS[i]
              return (
                <div className="lp-stat-card" key={label}>
                  <span className="lp-stat-icon"><Icon size={18} /></span>
                  <span className="lp-stat-value">{value}</span>
                  <span className="lp-stat-label">{label}</span>
                </div>
              )
            })}
          </div>
        </section>
      </main>

      <section className="lp-solution">
        <div className="lp-solution-header">
          <p className="lp-results-heading"><Sparkles size={13} /> How it works</p>
          <h2>Built for hiring teams that want clarity, speed, and fairness.</h2>
        </div>

        <div className="lp-solution-grid">
          <div className="lp-solution-card card-lift">
            <span className="lp-solution-number">01</span>
            <h3>Discover top talent</h3>
            <p>Match candidates with the right role based on skills, experience, and role fit.</p>
          </div>
          <div className="lp-solution-card card-lift">
            <span className="lp-solution-number">02</span>
            <h3>Screen intelligently</h3>
            <p>Use AI summaries and structured review signals to reduce manual effort.</p>
          </div>
          <div className="lp-solution-card card-lift">
            <span className="lp-solution-number">03</span>
            <h3>Hire with confidence</h3>
            <p>Track applications, interviews, and decisions in one clear recruiter workflow.</p>
          </div>
        </div>
      </section>

      {/* ── Dark feature band: matching + trust ── */}
      <section className="lp-dark-band">

        {/* Feature 1 — AI matching */}
        <div className="lp-feature">
          <div className="lp-feature-text">
            <span className="lp-badge lp-badge-dark">{t.feature1.badge}</span>
            <h2 className="lp-feature-title">
              {f1Prefix}<span className="lp-accent-light">{f1Accent}</span>
            </h2>
            <p className="lp-feature-eyebrow">{t.feature1.eyebrow}</p>
            <h3 className="lp-feature-subtitle">{t.feature1.subtitle}</h3>
            <p className="lp-feature-sub">{t.feature1.sub}</p>
          </div>

          <div className="lp-feature-visual">
            <div className="lp-card lp-card-float lp-card-score">
              <span className="lp-card-icon"><Target size={20} /></span>
              <span className="lp-card-big">92%</span>
              <span className="lp-card-caption">{t.feature1.scoreCaption}</span>
              <div className="lp-progress"><div className="lp-progress-bar" style={{ width: '92%' }} /></div>
            </div>

            <div className="lp-card lp-card-list">
              <div className="lp-card-list-head">
                <FileText size={16} /> {t.feature1.listHead}
              </div>
              {t.matches.map((m) => (
                <div className="lp-match-row" key={m.name}>
                  <div>
                    <p className="lp-match-name">{m.name}</p>
                    <p className="lp-match-role">{m.role}</p>
                  </div>
                  <span className="lp-match-score">{m.score}%</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Feature 2 — trust & verification */}
        <div className="lp-feature lp-feature-reverse">
          <div className="lp-feature-text">
            <span className="lp-badge lp-badge-dark">{t.feature2.badge}</span>
            <h2 className="lp-feature-title">
              {f2Prefix}<span className="lp-accent-light">{f2Accent}</span>
            </h2>
            <p className="lp-feature-eyebrow">{t.feature2.eyebrow}</p>
            <h3 className="lp-feature-subtitle">{t.feature2.subtitle}</h3>
            <p className="lp-feature-sub">{t.feature2.sub}</p>
          </div>

          <div className="lp-feature-visual">
            <div className="lp-card lp-card-list lp-card-wide">
              <div className="lp-card-list-head">
                <ShieldCheck size={16} /> {t.feature2.listHead}
              </div>
              {t.profiles.map((p) => (
                <div className={`lp-profile-row${p.status === 'flagged' ? ' lp-profile-row--flagged' : ''}`} key={p.name}>
                  <div>
                    <p className="lp-match-name">{p.name}</p>
                    <p className="lp-match-role">{p.role}</p>
                    {p.reasons && (
                      <div className="lp-flag-tags">
                        {p.reasons.map((r) => <span key={r} className="lp-flag-tag">{r}</span>)}
                      </div>
                    )}
                  </div>
                  <span className={`lp-status-badge${p.status === 'flagged' ? ' lp-status-badge--flag' : ''}`}>
                    {p.status === 'flagged' ? <Flag size={12} /> : <ShieldCheck size={12} />}
                    {p.status === 'flagged' ? t.statusFlagged : t.statusVerified}
                  </span>
                </div>
              ))}
            </div>

            <div className="lp-card lp-card-float lp-card-2fa">
              <span className="lp-card-icon"><KeyRound size={20} /></span>
              <span className="lp-card-caption-strong">{t.feature2.twoFaStrong}</span>
              <span className="lp-card-caption">{t.feature2.twoFaCaption}</span>
            </div>
          </div>
        </div>
      </section>

      {/* ── Regions ── */}
      <section className="lp-regions">
        <p className="lp-results-heading">{t.regionsHeading}</p>
        <h2 className="lp-regions-title">
          {regionsPrefix}<span className="lp-accent">{regionsAccent}</span>
        </h2>
        <div className="lp-regions-grid">
          {t.regions.map((label) => (
            <span className="lp-region-pill" key={label}>
              <span className="lp-region-icon"><Globe size={16} /></span>
              {label}
            </span>
          ))}
        </div>
      </section>

      {/* ── FAQ ── */}
      <section className="lp-faq">
        <h2 className="lp-faq-title">{t.faqTitle}</h2>
        <div className="lp-faq-list">
          {t.faqs.map((item, i) => (
            <FaqItem
              key={item.q}
              q={item.q}
              a={item.a}
              isOpen={openFaq === i}
              onToggle={() => setOpenFaq(openFaq === i ? -1 : i)}
            />
          ))}
        </div>
      </section>

      <section className="lp-cta-panel">
        <div className="lp-cta-panel-inner">
          <div>
            <p className="lp-results-heading"><Sparkles size={13} /> Ready to scale hiring?</p>
            <h2>Build a stronger hiring engine for your team.</h2>
          </div>
          <Link to="/register" className="lp-hero-cta lp-hero-cta--large">
            Start now <ArrowRight size={16} />
          </Link>
        </div>
      </section>

      <Footer />
    </div>
  )
}
