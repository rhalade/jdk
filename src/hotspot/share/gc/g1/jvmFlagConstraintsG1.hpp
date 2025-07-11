/*
 * Copyright (c) 2015, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef SHARE_GC_G1_JVMFLAGCONSTRAINTSG1_HPP
#define SHARE_GC_G1_JVMFLAGCONSTRAINTSG1_HPP

#include "runtime/flags/jvmFlag.hpp"
#include "utilities/globalDefinitions.hpp"

#define G1_GC_CONSTRAINTS(f)                          \
                                                      \
  /* G1 Remembered Sets Constraints */                \
  f(uint,   G1RemSetArrayOfCardsEntriesConstraintFunc)\
  f(uint,   G1RemSetHowlMaxNumBucketsConstraintFunc)  \
  f(uint,   G1RemSetHowlNumBucketsConstraintFunc)     \
                                                      \
  /* G1 Heap Size Constraints */                      \
  f(size_t, G1HeapRegionSizeConstraintFunc)           \
  f(uint,  G1NewSizePercentConstraintFunc)           \
  f(uint,  G1MaxNewSizePercentConstraintFunc)        \
                                                      \
  /* G1 Subconstraints */                             \
  f(uintx,  MaxGCPauseMillisConstraintFuncG1)         \
  f(uintx,  GCPauseIntervalMillisConstraintFuncG1)    \
  f(size_t, NewSizeConstraintFuncG1)                  \
                                                      \
  /* G1 PtrQueue buffer size constraints */           \
  f(size_t, G1SATBBufferSizeConstraintFunc)           \
  f(size_t, G1UpdateBufferSizeConstraintFunc)         \
                                                      \
  /* G1 GC deviation counter threshold constraints */ \
  f(uint, G1CPUUsageExpandConstraintFunc)             \
  f(uint, G1CPUUsageShrinkConstraintFunc)             \
  /* */

G1_GC_CONSTRAINTS(DECLARE_CONSTRAINT)

size_t MaxSizeForHeapAlignmentG1();

#endif // SHARE_GC_G1_JVMFLAGCONSTRAINTSG1_HPP
