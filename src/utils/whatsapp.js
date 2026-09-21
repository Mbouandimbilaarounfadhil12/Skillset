export function buildWhatsAppLink(phone, message) {
  if (!phone) return null
  const digits = phone.replace(/[^0-9]/g, '')
  if (!digits) return null
  const base = `https://wa.me/${digits}`
  return message ? `${base}?text=${encodeURIComponent(message)}` : base
}
