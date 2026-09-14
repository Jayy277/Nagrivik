import { apiRequest, RequestOptions } from './client';
import {
  AccountabilityFilterParams,
  PublicAccountabilityResponse,
} from '../../types/accountability';

export async function getPublicAccountability(
  params: AccountabilityFilterParams = {},
  options: RequestOptions = {}
): Promise<PublicAccountabilityResponse> {
  const queryParams = new URLSearchParams();
  if (params.cityId) queryParams.set('cityId', params.cityId);
  if (params.wardId) queryParams.set('wardId', params.wardId);
  if (params.categoryId) queryParams.set('categoryId', params.categoryId);
  if (params.range) queryParams.set('range', params.range);

  const queryStr = queryParams.toString();
  const endpoint = `/public/accountability${queryStr ? `?${queryStr}` : ''}`;

  return apiRequest<PublicAccountabilityResponse>(endpoint, options);
}
