import { Mail, Phone, MapPin, Share2, MessageCircle, Link, Code2 } from 'lucide-react'

const Footer = () => {
  const currentYear = new Date().getFullYear()

  return (
    <footer className="bg-gray-900 text-gray-300">
      {/* Main Footer Content */}
      <div className="max-w-7xl mx-auto px-6 py-16">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-12 mb-12">
          {/* Company Info */}
          <div>
            <div className="flex items-center gap-2 mb-4">
              <div className="w-8 h-8 bg-cherry-600 text-white font-bold text-sm rounded-lg flex items-center justify-center">
                S
              </div>
              <h3 className="text-lg font-bold text-white">SkillSet</h3>
            </div>
            <p className="text-sm leading-relaxed mb-6">
              Plateforme de recrutement moderne connectant talents et entreprises.
            </p>
            <div className="flex items-center gap-4">
              <a href="#facebook" aria-label="Facebook" className="text-gray-400 hover:text-cherry-600 transition">
                <Share2 size={20} />
              </a>
              <a href="#twitter" aria-label="Twitter" className="text-gray-400 hover:text-cherry-600 transition">
                <MessageCircle size={20} />
              </a>
              <a href="#linkedin" aria-label="LinkedIn" className="text-gray-400 hover:text-cherry-600 transition">
                <Link size={20} />
              </a>
              <a href="#github" aria-label="GitHub" className="text-gray-400 hover:text-cherry-600 transition">
                <Code2 size={20} />
              </a>
            </div>
          </div>

          {/* Quick Links */}
          <div>
            <h4 className="font-semibold text-white mb-4">Liens Rapides</h4>
            <ul className="space-y-2 text-sm">
              <li><a href="/" className="text-gray-400 hover:text-cherry-600 transition">Accueil</a></li>
              <li><a href="/jobs" className="text-gray-400 hover:text-cherry-600 transition">Emplois</a></li>
              <li><a href="/candidates" className="text-gray-400 hover:text-cherry-600 transition">Candidats</a></li>
              <li><a href="/about" className="text-gray-400 hover:text-cherry-600 transition">À Propos</a></li>
            </ul>
          </div>

          {/* Resources */}
          <div>
            <h4 className="font-semibold text-white mb-4">Ressources</h4>
            <ul className="space-y-2 text-sm">
              <li><a href="/blog" className="text-gray-400 hover:text-cherry-600 transition">Blog</a></li>
              <li><a href="/faq" className="text-gray-400 hover:text-cherry-600 transition">FAQ</a></li>
              <li><a href="/help" className="text-gray-400 hover:text-cherry-600 transition">Aide</a></li>
              <li><a href="/contact" className="text-gray-400 hover:text-cherry-600 transition">Contact</a></li>
            </ul>
          </div>

          {/* Contact Info */}
          <div>
            <h4 className="font-semibold text-white mb-4">Contact</h4>
            <div className="space-y-3 text-sm">
              <div className="flex items-center gap-3">
                <Mail size={18} className="text-cherry-600 flex-shrink-0" />
                <span>contact@skillset.africa</span>
              </div>
              <div className="flex items-center gap-3">
                <Phone size={18} className="text-cherry-600 flex-shrink-0" />
                <span>+225 07 12 34 56 78</span>
              </div>
              <div className="flex items-center gap-3">
                <MapPin size={18} className="text-cherry-600 flex-shrink-0" />
                <span>Abidjan, Côte d&apos;Ivoire</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Bar */}
      <div className="border-t border-gray-800">
        <div className="max-w-7xl mx-auto px-6 py-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-sm">
          <p className="text-gray-400">
            © {currentYear} SkillSet. Tous droits réservés.
          </p>
          <div className="flex items-center gap-4 text-gray-400">
            <a href="/privacy" className="hover:text-cherry-600 transition">Politique de Confidentialité</a>
            <span>•</span>
            <a href="/terms" className="hover:text-cherry-600 transition">Conditions d'Utilisation</a>
            <span>•</span>
            <a href="/cookies" className="hover:text-cherry-600 transition">Gestion des Cookies</a>
          </div>
        </div>
      </div>
    </footer>
  )
}

export default Footer
