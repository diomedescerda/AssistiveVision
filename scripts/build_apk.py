#!/usr/bin/env python3
"""Build a tested debug APK and package it with checksums and source provenance."""
from __future__ import annotations
import argparse
from datetime import datetime,timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

from verify_assets import ROOT,verify


def sha256(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--output',type=Path,default=ROOT/'dist')
    a=p.parse_args();output=a.output.resolve()
    if output.exists():p.error('Output already exists. Choose a fresh --output directory.')
    sdk_path=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk_path:p.error('Set ANDROID_HOME to your Android SDK directory (see docs/REPRODUCING.md).')
    sdk=Path(sdk_path)
    if not (sdk/'platforms/android-37.1/android.jar').is_file():p.error('Install Android SDK Platform 37.1 in Android Studio SDK Manager.')
    tools=sdk/'build-tools/36.0.0'
    if not tools.is_dir():p.error('Install Android SDK Build-Tools 36.0.0 for APK validation.')
    assets=verify()
    java=Path(os.environ['JAVA_HOME'])/'bin/java' if os.environ.get('JAVA_HOME') else Path('java')
    java_info=subprocess.run([str(java),'-version'],check=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True).stdout
    wrapper=ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')
    command=[str(wrapper),'--no-daemon','--max-workers=2','clean',':app:testDebugUnitTest',':app:lintDebug',':app:assembleDebug',':app:assembleDebugAndroidTest']
    # Clean prevents stale APKs and stale test results from being packaged.
    subprocess.run(command,cwd=ROOT,check=True)
    apk=ROOT/'app/build/outputs/apk/debug/app-debug.apk'
    with zipfile.ZipFile(apk) as archive:
        for name,digest in assets.items():
            if hashlib.sha256(archive.read('assets/'+name)).hexdigest()!=digest:
                raise ValueError(f'APK asset differs from source: {name}')
    apksigner=tools/('apksigner.bat' if os.name=='nt' else 'apksigner')
    signing_check=subprocess.check_output([str(apksigner),'verify','--verbose','--print-certs',str(apk)],text=True)
    aapt=tools/('aapt.exe' if os.name=='nt' else 'aapt')
    badging=subprocess.check_output([str(aapt),'dump','badging',str(apk)],text=True)
    if "uses-permission: name='android.permission.INTERNET'" in badging:
        raise ValueError('Unexpected INTERNET permission in the merged APK manifest.')
    if "sdkVersion:'26'" not in badging or "native-code: 'arm64-v8a' 'x86_64'" not in badging:
        raise ValueError('APK minimum SDK or supported ABIs differ from the documented package.')
    output.mkdir(parents=True)
    target=output/'AssistiveVision-1.0-debug.apk';shutil.copy2(apk,target)
    files=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=ROOT).decode().split('\0')
    source={name:sha256(ROOT/name) for name in sorted(set(files))
            if name and (ROOT/name).is_file() and output not in (ROOT/name).resolve().parents}
    git_commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
    manifest=dict(built_at=datetime.now(timezone.utc).isoformat(),variant='debug',
        application_id='com.unimagdalena.assistivevision',version_name='1.0',version_code=1,
        source_commit=git_commit,
        source_worktree_changed=bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()),
        source_tree_sha256=hashlib.sha256(json.dumps(source,sort_keys=True).encode()).hexdigest(),
        source_files=source,java=java_info,gradle_command=command,
        apk=dict(file=target.name,bytes=target.stat().st_size,sha256=sha256(target)),
        bundled_asset_count=len(assets),supported_abis=['arm64-v8a','x86_64'],minimum_android_api=26,
        validation=['clean build','debug unit tests','debug Android lint','APK bundled-asset checksum comparison','APK signature verification','minimum SDK, ABIs and no INTERNET permission'],
        signing='Android debug certificate; local research/testing, not a production release',
        reproducibility='Rebuildable from source; APK bytes differ with signing keys and build environment.')
    (output/'build-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    (output/'SHA256SUMS').write_text(sha256(target)+'  '+target.name+'\n')
    (output/'apk-signature.txt').write_text(signing_check)
    (output/'apk-metadata.txt').write_text(badging)
    shutil.copy2(ROOT/'LICENSE',output/'LICENSE')
    shutil.copy2(ROOT/'docs/THIRD_PARTY.md',output/'THIRD_PARTY.md')
    print(f'APK ready: {target}\nSHA-256: {sha256(target)}')


if __name__=='__main__':main()
