import { useEffect, useRef, useState } from 'react'
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

async function reverseGeocode(lat, lng) {
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json&accept-language=fr`
    )
    const data = await res.json()
    return data.display_name || ''
  } catch {
    return ''
  }
}

// Stable external component — reads callback from a ref so its identity never changes
function MapClickEvents({ handlerRef }) {
  useMapEvents({
    click(e) { handlerRef.current?.(e.latlng.lat, e.latlng.lng) },
  })
  return null
}

export default function MapPicker({ value, onChange, onAddressFound }) {
  const [position, setPosition]       = useState(null)
  const [address, setAddress]         = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [searching, setSearching]     = useState(false)
  const mapRef     = useRef(null)
  const handlerRef = useRef(null)   // stable ref for the click handler

  // Parse initial value once on mount — defer setState to avoid synchronous effect cascade
  useEffect(() => {
    if (value?.includes(',')) {
      const [lat, lng] = value.split(',').map(Number)
      if (!Number.isNaN(lat) && !Number.isNaN(lng)) {
        const t = setTimeout(() => setPosition([lat, lng]), 0)
        mapRef.current?.setView([lat, lng], 14)
        return () => clearTimeout(t)
      }
    }
    return undefined
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  // Keep handlerRef current — must be in useEffect, not render body
  useEffect(() => {
    handlerRef.current = async (lat, lng) => {
      const round = n => Math.round(n * 1e6) / 1e6
      const rlat = round(lat)
      const rlng = round(lng)
      setPosition([rlat, rlng])
      onChange(`${rlat},${rlng}`)

      const fullAddress = await reverseGeocode(rlat, rlng)
      if (fullAddress) {
        setAddress(fullAddress)
        onAddressFound?.(fullAddress)
      }
    }
  }, [onChange, onAddressFound])

  const handleSearch = async () => {
    if (!searchInput.trim()) return
    setSearching(true)
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(searchInput)}&format=json&limit=1`,
        { headers: { 'Accept-Language': 'fr' } }
      )
      const data = await res.json()
      if (data.length > 0) {
        const lat = Number.parseFloat(data[0].lat)
        const lng = Number.parseFloat(data[0].lon)
        mapRef.current?.setView([lat, lng], 15)
        await handlerRef.current?.(lat, lng)
      }
    } catch {
      // ignore geocoding errors silently
    } finally {
      setSearching(false)
    }
  }

  return (
    <div className="map-picker">
      <div className="map-picker-search">
        <input
          className="ob-input"
          placeholder="Rechercher une adresse ou ville…"
          value={searchInput}
          onChange={e => setSearchInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSearch()}
        />
        <button type="button" className="map-picker-btn" onClick={handleSearch} disabled={searching}>
          {searching ? '…' : 'Localiser'}
        </button>
      </div>

      <MapContainer
        center={[20, 0]}
        zoom={2}
        style={{ height: '280px', width: '100%', borderRadius: '10px', marginTop: '8px' }}
        ref={mapRef}
      >
        <TileLayer
          attribution='© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <MapClickEvents handlerRef={handlerRef} />
        {position && <Marker position={position} />}
      </MapContainer>

      {address && (
        <p className="map-picker-address">
          <strong>Adresse :</strong> {address}
        </p>
      )}
      {!address && position && (
        <p className="map-picker-coords">
          Coordonnées : {position[0].toFixed(5)}, {position[1].toFixed(5)}
        </p>
      )}
      {!address && !position && (
        <p className="map-picker-hint">
          Cliquez sur la carte ou recherchez une adresse pour placer votre siège social
        </p>
      )}
    </div>
  )
}
