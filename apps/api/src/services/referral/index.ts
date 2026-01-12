/**
 * Referral Service Module
 *
 * This module has been refactored into three focused services:
 * - ReferralService: Core CRUD operations and business logic
 * - ReferralBullhornService: Bullhorn ATS integration
 * - ConsultantAssignmentService: Consultant selection algorithms
 */

export { ReferralService, referralService } from './referral.service';
export { ReferralBullhornService, referralBullhornService } from './referral-bullhorn.service';
export { ConsultantAssignmentService, consultantAssignmentService } from './consultant-assignment.service';
