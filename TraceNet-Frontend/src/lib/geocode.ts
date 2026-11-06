/**
 * @file geocode.ts
 * @description Geolocation detection and reverse-geocoding utilities using the HTML5
 * Geolocation API and OpenStreetMap Nominatim API for resolving addresses, cities, and states.
 */

/**
 * Structured geographic address resolved from latitude and longitude coordinates.
 */
export type GeoAddress = {
  lat: number;
  lng: number;
  address?: string;
  city?: string;
  state?: string;
  postcode?: string;
  country?: string;
  displayName?: string;
};

/**
 * Promisified wrapper around `navigator.geolocation.getCurrentPosition`.
 *
 * @param options Geolocation options (accuracy, timeout, maximumAge).
 * @returns Promise resolving to the browser GeolocationPosition.
 */
export function getCurrentPosition(options?: PositionOptions): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      reject(new Error("Geolocation is not supported by this browser."));
      return;
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 12000,
      maximumAge: 60_000,
      ...options,
    });
  });
}

/**
 * Performs reverse-geocoding lookup via OpenStreetMap Nominatim service.
 *
 * @param lat Latitude in degrees.
 * @param lng Longitude in degrees.
 * @returns Promise resolving to a structured {@link GeoAddress}.
 */
export async function reverseGeocode(lat: number, lng: number): Promise<GeoAddress> {
  const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}&addressdetails=1&zoom=18&email=contact@tracenest.app`;
  const res = await fetch(url, { headers: { Accept: "application/json" } });
  if (!res.ok) throw new Error("Failed to look up address");
  const data = await res.json();
  const a = data.address ?? {};
  const streetParts = [a.house_number, a.road].filter(Boolean).join(" ");
  const areaParts = [a.neighbourhood, a.suburb, a.village, a.hamlet].filter(Boolean).join(", ");
  const address = [streetParts, areaParts].filter(Boolean).join(", ") || data.display_name;
  return {
    lat,
    lng,
    address,
    city: a.city || a.town || a.village || a.municipality || a.county,
    state: a.state || a.region,
    postcode: a.postcode,
    country: a.country,
    displayName: data.display_name,
  };
}

/**
 * One-click helper detecting user GPS coordinates and automatically resolving the reverse address.
 *
 * @returns Promise resolving to {@link GeoAddress} with address or bare coordinates if lookup fails.
 */
export async function detectLocation(): Promise<GeoAddress> {
  const pos = await getCurrentPosition();
  const { latitude, longitude } = pos.coords;
  try {
    return await reverseGeocode(latitude, longitude);
  } catch {
    return { lat: latitude, lng: longitude };
  }
}
