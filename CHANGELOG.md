# Changelog
All notable changes to this project will be documented in this file. Also see [MIGRATIONS.md](MIGRATIONS.md) when upgrading the ACT platform to a newer version.

The format is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Origin, Trust & Confidence] - 2019-09-18
### Added
- REST endpoint for retrieval of an Origin by ID, `GET /v1/origin/uuid/{id}`.
- REST endpoint for listing Origins, `GET /v1/origin`.
- REST endpoint for creating Origins, `POST /v1/origin`.
- REST endpoint for updating Origins, `PUT /v1/origin/uuid/{id}`.
- REST endpoint for deleting Origins, `DELETE /v1/origin/uuid/{id}`.

### Changed
- Extend FactType API to include `defaultConfidence` field.
- Extend Fact API to include `trust`, `confidence`, `certainty` and `addedBy` fields.
- Extend Search APIs to filter on `trust`, `confidence` and `certainty`.
- Rename field `source` to `origin` in Fact and Search APIs (**breaking**).

## [Miscellaneous]
### Changed
- (2020-04-17) In Create APIs (create Fact, create meta Fact, retract Fact) related entities (`organization`, `origin`, `acl`)
can be specified by either UUID or name. If an entity cannot be resolved a 412 response will be returned.
- (2020-04-17) When granting a Subject access to an existing Fact the Subject can be specified by either UUID or name.
If the Subject cannot be resolved a 412 response will be returned.
- (2019-11-06) If a user has the `unlimitedSearch` permission an unlimited number of results can be fetched when
searching for Facts. Otherwise the maximum number of returned results is capped at 10.000.
- (2019-11-04) Creating a new Fact where `source` and `destination` are the same Object will fail with a 412 response.
For this use case one-legged Facts with bi-directional binding should be created instead.
- (2019-09-25) In Search APIs resolve entities by name for `factType`, `objectType`, `origin` and `organization` before querying
ElasticSearch in order to avoid outdated names in the index. If an entity cannot be resolved a 412 response will be returned.
