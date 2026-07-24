# Dependency, license and SBOM policy

`dependency-policy.json` is the reviewed allowlist for dependencies that can be
part of the Android release. It covers resolved Maven runtime components,
checksum-pinned native source packages, and native code vendored directly in
the repository.

The allowlist records engineering evidence, not a legal opinion. A new Maven
group, native CMake target or license expression fails CI until its source,
license, release obligations and intended use are reviewed and added
explicitly. `NOASSERTION` is accepted only as a download location for legacy
vendored code; it is never accepted as a license.

## Generate the Android release SBOM

```bash
cd android
./gradlew :app:writeReleaseDependencyInventory
cd ..
python3 tools/generate_dependency_sbom.py \
  --gradle-report android/app/build/reports/dependencies/release-runtime.json \
  --output android/app/build/reports/sbom/coolreader-android.spdx.json
```

The output is SPDX 2.3 JSON. CI publishes it as an artifact. It describes
resolved Android release-runtime Maven components and native source linked or
prepared by the Android build. Android platform libraries are not copied into
the app and are not listed as shipped packages.

Desktop packages use system libraries on verified Linux/macOS jobs. Their exact
package versions belong in the release provenance for the environment that
builds a desktop archive; they must not be guessed from the repository-level
policy.

## Review checklist

When adding or updating a dependency:

1. Pin its version and, for downloaded native sources, its SHA-512.
2. Verify the license from the released source/POM, including bundled test data
   and fonts.
3. Confirm compatibility and notice/source obligations for static and dynamic
   linking.
4. Add the narrowest Maven group rule or exact native component record.
5. Regenerate the inventory/SBOM and inspect the package/relationship diff.
6. Update user-facing notices and release source offer where required.

Vulnerability scanning is intentionally not performed by this workflow; it is
tracked in the separate security workstream in `MASTER_PLAN.md`.
