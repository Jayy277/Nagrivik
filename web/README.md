# Nagrivic Web Applications

This directory contains the planned web applications for the Nagrivic platform.

## Applications Overview

1. **Public Website (`web/public`)**:
   - **Framework**: Next.js + React + TypeScript
   - **Target Audience**: Citizens, researchers, and media.
   - **Key Features**:
     - Ward-level civic transparency leaderboards
     - Interactive maps of reported and resolved civic issues
     - SEO-optimized public detail pages for each reported issue
     - Real-time city health metrics (e.g., average resolution time per category)

2. **Admin & Authority Dashboard (`web/admin`)**:
   - **Framework**: Next.js + React + TypeScript
   - **Target Audience**: Urban Local Body (ULB) officials, ward engineers, system moderators.
   - **Key Features**:
     - Moderation queue for citizen-submitted reports
     - Department ticket assignment and SLA tracking
     - Geographic heatmap analysis of recurring civic problems
     - Resolution verification management

## Architecture Principles

- Communicate exclusively with the central Spring Boot REST APIs (`/api/*`).
- No direct database access.
- Shared TypeScript interfaces with the backend REST response contracts.
