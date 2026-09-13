/**
 * Nagrivic Geographic City Configuration (Web)
 * Default public map context for civic issue discovery.
 * Initial pilot is Ahmedabad, Gujarat, India.
 * Architecture allows seamless extension to Surat, Vadodara, Rajkot, etc.
 */

export interface CityConfig {
  id: string;
  name: string;
  state: string;
  country: string;
  latitude: number;
  longitude: number;
  defaultRadiusMeters: number;
  defaultZoom: number;
}

export const AHMEDABAD_CITY_CONFIG: CityConfig = {
  id: 'ahmedabad',
  name: 'Ahmedabad',
  state: 'Gujarat',
  country: 'India',
  // Official AMC / Municipal Corporation center coordinate
  latitude: 23.0225,
  longitude: 72.5714,
  defaultRadiusMeters: 5000,
  defaultZoom: 13,
};

export const SUPPORTED_CITIES: Record<string, CityConfig> = {
  ahmedabad: AHMEDABAD_CITY_CONFIG,
};

export function getDefaultCityConfig(): CityConfig {
  const envLat = process.env.NEXT_PUBLIC_DEFAULT_CITY_LAT;
  const envLng = process.env.NEXT_PUBLIC_DEFAULT_CITY_LNG;
  const envRadius = process.env.NEXT_PUBLIC_DEFAULT_CITY_RADIUS_METERS;

  if (envLat && envLng && !isNaN(Number(envLat)) && !isNaN(Number(envLng))) {
    return {
      ...AHMEDABAD_CITY_CONFIG,
      latitude: Number(envLat),
      longitude: Number(envLng),
      defaultRadiusMeters: envRadius && !isNaN(Number(envRadius)) ? Number(envRadius) : 5000,
    };
  }

  return AHMEDABAD_CITY_CONFIG;
}
