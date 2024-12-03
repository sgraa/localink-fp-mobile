// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    repositories {
        google() // Ensure the repository for Google services is included
        mavenCentral()
    }
    dependencies {
        // The classpath for Google services plugin
        classpath("com.google.gms:google-services:4.4.2")
    }
}

allprojects {
    repositories {
    }
}

