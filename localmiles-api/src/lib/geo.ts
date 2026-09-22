const EARTH_RADIUS_KM = 6371;

export interface Point {
  lat: number;
  lng: number;
}

function toRad(deg: number): number {
  return (deg * Math.PI) / 180;
}

// Great-circle ("as the crow flies") distance between two lat/lng points, in km.
// Chris Veness, "Calculate distance and bearing between two Latitude/Longitude points using haversine formula in JavaScript", 
// https://www.movable-type.co.uk/scripts/latlong.html, 2002
export function haversineKm(a: Point, b: Point): number {
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const lat1 = toRad(a.lat);
  const lat2 = toRad(b.lat);

  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;

  return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
}
