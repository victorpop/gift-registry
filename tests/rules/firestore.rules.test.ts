import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
  RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import { collection, doc, getDoc, getDocs, query, setDoc, where } from "firebase/firestore";
import * as fs from "fs";

let testEnv: RulesTestEnvironment;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "gift-registry-test",
    firestore: {
      rules: fs.readFileSync("../../firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8080,
    },
  });
});

afterEach(async () => {
  await testEnv.clearFirestore();
});

afterAll(async () => {
  await testEnv.cleanup();
});

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

async function seedRegistry(
  id: string,
  data: Record<string, unknown>
): Promise<void> {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), "registries", id), data);
  });
}

async function seedUser(
  id: string,
  data: Record<string, unknown>
): Promise<void> {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), "users", id), data);
  });
}

// ─────────────────────────────────────────────────────────────────────────────
// describe("Public registry read")
// ─────────────────────────────────────────────────────────────────────────────

describe("Public registry read", () => {
  it("allows unauthenticated read of a public registry", async () => {
    await seedRegistry("pub1", {
      ownerId: "owner1",
      visibility: "public",
      title: "Public",
      invitedUsers: {},
    });

    const unauthDb = testEnv.unauthenticatedContext().firestore();
    await assertSucceeds(getDoc(doc(unauthDb, "registries", "pub1")));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("Private registry access")
// ─────────────────────────────────────────────────────────────────────────────

describe("Private registry access", () => {
  beforeEach(async () => {
    await seedRegistry("priv1", {
      ownerId: "owner2",
      visibility: "private",
      title: "Private",
      invitedUsers: { "invited-user": true },
    });
  });

  it("denies non-owner non-invited user", async () => {
    const db = testEnv.authenticatedContext("random-user").firestore();
    await assertFails(getDoc(doc(db, "registries", "priv1")));
  });

  it("allows owner to read their own private registry", async () => {
    const db = testEnv.authenticatedContext("owner2").firestore();
    await assertSucceeds(getDoc(doc(db, "registries", "priv1")));
  });

  it("allows invited user to read private registry", async () => {
    const db = testEnv.authenticatedContext("invited-user").firestore();
    await assertSucceeds(getDoc(doc(db, "registries", "priv1")));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("Reservation collection")
// ─────────────────────────────────────────────────────────────────────────────

describe("Reservation collection", () => {
  it("denies unauthenticated write to reservations", async () => {
    const unauthDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(
      setDoc(doc(unauthDb, "reservations", "res1"), {
        item: "test",
        userId: "anon",
      })
    );
  });

  it("denies authenticated read from reservations", async () => {
    const db = testEnv.authenticatedContext("any-user").firestore();
    await assertFails(getDoc(doc(db, "reservations", "res1")));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("Items subcollection")
// ─────────────────────────────────────────────────────────────────────────────

describe("Items subcollection", () => {
  beforeEach(async () => {
    await seedRegistry("pub1", {
      ownerId: "owner1",
      visibility: "public",
      invitedUsers: {},
    });
  });

  it("denies non-owner write to items subcollection", async () => {
    const db = testEnv.authenticatedContext("other-user").firestore();
    await assertFails(
      setDoc(doc(db, "registries", "pub1", "items", "item1"), {
        name: "Gift",
        url: "https://example.com",
      })
    );
  });

  it("allows owner to write to items subcollection", async () => {
    const db = testEnv.authenticatedContext("owner1").firestore();
    await assertSucceeds(
      setDoc(doc(db, "registries", "pub1", "items", "item1"), {
        name: "Gift",
        url: "https://example.com",
      })
    );
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("Users collection")
// ─────────────────────────────────────────────────────────────────────────────

describe("Users collection", () => {
  beforeEach(async () => {
    await seedUser("user1", {
      email: "test@example.com",
      displayName: "Test",
    });
  });

  it("allows user to read their own document", async () => {
    const db = testEnv.authenticatedContext("user1").firestore();
    await assertSucceeds(getDoc(doc(db, "users", "user1")));
  });

  it("denies user from reading another user's document", async () => {
    const db = testEnv.authenticatedContext("user2").firestore();
    await assertFails(getDoc(doc(db, "users", "user1")));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("Registry creation")
// ─────────────────────────────────────────────────────────────────────────────

describe("Registry creation", () => {
  it("allows create when ownerId matches auth uid", async () => {
    const db = testEnv.authenticatedContext("creator1").firestore();
    await assertSucceeds(
      setDoc(doc(db, "registries", "new1"), {
        ownerId: "creator1",
        visibility: "public",
        title: "New Registry",
        invitedUsers: {},
      })
    );
  });

  it("denies create when ownerId does not match auth uid", async () => {
    const db = testEnv.authenticatedContext("creator1").firestore();
    await assertFails(
      setDoc(doc(db, "registries", "new2"), {
        ownerId: "someone-else",
        visibility: "public",
        title: "Fraudulent Registry",
        invitedUsers: {},
      })
    );
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("Private registry invite flow")
// ─────────────────────────────────────────────────────────────────────────────

describe("Private registry invite flow", () => {
  it("invited user can read private registry", async () => {
    await seedRegistry("private-invite-reg", {
      ownerId: "owner-invite-test",
      title: "Private Party",
      occasion: "birthday",
      visibility: "private",
      invitedUsers: { "invited-user-1": true },
      notificationsEnabled: true,
      locale: "en",
      createdAt: Date.now(),
      updatedAt: Date.now(),
    });

    const invitedDb = testEnv.authenticatedContext("invited-user-1").firestore();
    await assertSucceeds(
      getDoc(doc(invitedDb, "registries", "private-invite-reg"))
    );
  });

  it("non-invited user cannot read private registry", async () => {
    await seedRegistry("private-noinvite-reg", {
      ownerId: "some-owner",
      title: "Private Secret",
      occasion: "wedding",
      visibility: "private",
      invitedUsers: { "other-uid": true },
      notificationsEnabled: true,
      locale: "en",
      createdAt: Date.now(),
      updatedAt: Date.now(),
    });

    const nonInvitedDb = testEnv.authenticatedContext("non-invited-user-1").firestore();
    await assertFails(
      getDoc(doc(nonInvitedDb, "registries", "private-noinvite-reg"))
    );
  });

  it("invited user can read items in private registry", async () => {
    await seedRegistry("private-items-reg", {
      ownerId: "items-owner",
      title: "Items Test",
      occasion: "christmas",
      visibility: "private",
      invitedUsers: { "invited-user-items": true },
      notificationsEnabled: true,
      locale: "en",
      createdAt: Date.now(),
      updatedAt: Date.now(),
    });

    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), "registries", "private-items-reg", "items", "item-1"),
        {
          title: "Gift Item",
          originalUrl: "https://example.com",
          affiliateUrl: "https://example.com",
          status: "available",
          createdAt: Date.now(),
          updatedAt: Date.now(),
        }
      );
    });

    const invitedDb = testEnv.authenticatedContext("invited-user-items").firestore();
    await assertSucceeds(
      getDoc(doc(invitedDb, "registries", "private-items-reg", "items", "item-1"))
    );
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("Phase 4: Items status field read")
// ─────────────────────────────────────────────────────────────────────────────

describe("Phase 4: Items status field read (RES-02/RES-06)", () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), "registries", "p4-reg"),
        { ownerId: "owner-p4", visibility: "public", invitedUsers: {} }
      );
      await setDoc(
        doc(ctx.firestore(), "registries", "p4-reg", "items", "item-1"),
        { title: "Gift", status: "available", affiliateUrl: "https://example.com" }
      );
    });
  });

  it("allows unauthenticated read of item status (RES-02/RES-06)", async () => {
    const unauthDb = testEnv.unauthenticatedContext().firestore();
    await assertSucceeds(
      getDoc(doc(unauthDb, "registries", "p4-reg", "items", "item-1"))
    );
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("Phase 4: Reservations hard-deny extended")
// ─────────────────────────────────────────────────────────────────────────────

describe("Phase 4: Reservations hard-deny extended (D-19/RES-09)", () => {
  it("denies authenticated write to reservations collection", async () => {
    const db = testEnv.authenticatedContext("any-uid").firestore();
    await assertFails(
      setDoc(doc(db, "reservations", "res-auth"), { status: "active", itemId: "i", registryId: "r" })
    );
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("Phase 6: mail collection (D-22)")
// ─────────────────────────────────────────────────────────────────────────────

describe("Phase 6: mail collection (D-22)", () => {
  it("denies unauthenticated write to mail", async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(setDoc(doc(db, "mail", "m1"), {
      to: "a@b.com", message: { subject: "s", html: "h", text: "t" },
    }));
  });

  it("denies authenticated write to mail", async () => {
    const db = testEnv.authenticatedContext("any-uid").firestore();
    await assertFails(setDoc(doc(db, "mail", "m1"), {
      to: "a@b.com", message: { subject: "s", html: "h", text: "t" },
    }));
  });

  it("denies authenticated read from mail", async () => {
    const db = testEnv.authenticatedContext("any-uid").firestore();
    await assertFails(getDoc(doc(db, "mail", "m1")));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("Phase 6: notifications_failures (D-22)")
// ─────────────────────────────────────────────────────────────────────────────

describe("Phase 6: notifications_failures (D-22)", () => {
  it("denies authenticated read", async () => {
    const db = testEnv.authenticatedContext("any-uid").firestore();
    await assertFails(getDoc(doc(db, "notifications_failures", "f1")));
  });

  it("denies authenticated write", async () => {
    const db = testEnv.authenticatedContext("any-uid").firestore();
    await assertFails(setDoc(doc(db, "notifications_failures", "f1"), { type: "test" }));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("Phase 6: users/{uid}/fcmTokens (D-22)")
// ─────────────────────────────────────────────────────────────────────────────

describe("Phase 6: users/{uid}/fcmTokens (D-22)", () => {
  it("allows owner to write their own fcmToken", async () => {
    const db = testEnv.authenticatedContext("owner-u1").firestore();
    await assertSucceeds(
      setDoc(doc(db, "users", "owner-u1", "fcmTokens", "tok1"), {
        token: "tok1", platform: "android", createdAt: Date.now(), lastSeenAt: Date.now(),
      })
    );
  });

  it("allows owner to read their own fcmToken", async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "users", "owner-u1", "fcmTokens", "tok1"), { token: "tok1" });
    });
    const db = testEnv.authenticatedContext("owner-u1").firestore();
    await assertSucceeds(getDoc(doc(db, "users", "owner-u1", "fcmTokens", "tok1")));
  });

  it("denies other user from reading another user's fcmToken", async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "users", "owner-u2", "fcmTokens", "tok2"), { token: "tok2" });
    });
    const db = testEnv.authenticatedContext("attacker-u").firestore();
    await assertFails(getDoc(doc(db, "users", "owner-u2", "fcmTokens", "tok2")));
  });

  it("denies other user from writing to another user's fcmTokens", async () => {
    const db = testEnv.authenticatedContext("attacker-u").firestore();
    await assertFails(
      setDoc(doc(db, "users", "victim-u", "fcmTokens", "tokX"), { token: "tokX" })
    );
  });

  it("denies unauthenticated read of any fcmTokens", async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, "users", "any-u", "fcmTokens", "any-tok")));
  });
});

// Plan 17-01 (D-44): removed the "config/{configId} rules" describe block
// alongside the firestore.rules `match /config/{configId}` block deletion.
// The Stores capability was decommissioned and config/stores no longer exists.

// ─────────────────────────────────────────────────────────────────────────────
// describe("Legacy registry docs (missing fields)")
// Regression guard for production PERMISSION_DENIED on list evaluation:
//   "Property visibility is undefined on object. for 'list' @ L32"
// ─────────────────────────────────────────────────────────────────────────────

describe("Legacy registry docs (missing fields)", () => {
  it("allows owner to list legacy registry missing `visibility`", async () => {
    await seedRegistry("legacy-no-vis", {
      ownerId: "owner-legacy-1",
      title: "Legacy",
      invitedUsers: {},
      // visibility intentionally absent
    });
    const db = testEnv.authenticatedContext("owner-legacy-1").firestore();
    const q = query(
      collection(db, "registries"),
      where("ownerId", "==", "owner-legacy-1")
    );
    await assertSucceeds(getDocs(q));
  });

  it("allows owner to list legacy registry missing `invitedUsers`", async () => {
    await seedRegistry("legacy-no-invites", {
      ownerId: "owner-legacy-2",
      visibility: "private",
      title: "Legacy private",
      // invitedUsers intentionally absent
    });
    const db = testEnv.authenticatedContext("owner-legacy-2").firestore();
    const q = query(
      collection(db, "registries"),
      where("ownerId", "==", "owner-legacy-2")
    );
    await assertSucceeds(getDocs(q));
  });

  it("denies read of a doc missing `ownerId` (fail-closed)", async () => {
    await seedRegistry("legacy-no-owner", {
      visibility: "private",
      title: "Orphan",
      invitedUsers: {},
      // ownerId intentionally absent
    });
    const db = testEnv.authenticatedContext("any-user").firestore();
    await assertFails(getDoc(doc(db, "registries", "legacy-no-owner")));
  });

  it("regression: public registry remains readable by unauthenticated users", async () => {
    await seedRegistry("pub-reg", {
      ownerId: "owner-p",
      visibility: "public",
      title: "Public",
      invitedUsers: {},
    });
    const unauthDb = testEnv.unauthenticatedContext().firestore();
    await assertSucceeds(getDoc(doc(unauthDb, "registries", "pub-reg")));
  });

  it("regression: private registry remains readable by invited user", async () => {
    await seedRegistry("priv-reg", {
      ownerId: "owner-priv",
      visibility: "private",
      title: "Private",
      invitedUsers: { "guest-user": true },
    });
    const db = testEnv.authenticatedContext("guest-user").firestore();
    await assertSucceeds(getDoc(doc(db, "registries", "priv-reg")));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("pendingInvitedUsers read scope (D-18)")
//
// Plan 16-01 Wave 0 — verifies that the new `pendingInvitedUsers` map added
// by D-23 does NOT grant read access to a registry. The isInvited rule only
// reads `invitedUsers`; `pendingInvitedUsers` is invisible to it. These tests
// pass against the EXISTING rules — no rule edit needed (Pattern 8).
//
// D-19 sub-test confirms the rule still grants access AFTER the
// acceptInvite callable promotes a pending entry into invitedUsers.
// ─────────────────────────────────────────────────────────────────────────────

describe("pendingInvitedUsers read scope (D-18)", () => {
  it("non-owner cannot read a registry doc with pendingInvitedUsers populated", async () => {
    await seedRegistry("reg-pending", {
      ownerId: "owner-1",
      visibility: "private",
      invitedUsers: {},
      pendingInvitedUsers: { "stranger-uid": true },
    });
    const strangerDb = testEnv.authenticatedContext("stranger-uid").firestore();
    await assertFails(getDoc(doc(strangerDb, "registries", "reg-pending")));
  });

  it("invitee with ONLY pending entry (no invitedUsers entry) cannot read registry", async () => {
    await seedRegistry("reg-pending-only", {
      ownerId: "owner-1",
      visibility: "private",
      invitedUsers: {},
      pendingInvitedUsers: { "invitee-uid": true },
    });
    const inviteeDb = testEnv.authenticatedContext("invitee-uid").firestore();
    await assertFails(getDoc(doc(inviteeDb, "registries", "reg-pending-only")));
  });

  it("owner CAN read registry with pendingInvitedUsers populated", async () => {
    await seedRegistry("reg-owner-pending", {
      ownerId: "owner-1",
      visibility: "private",
      invitedUsers: {},
      pendingInvitedUsers: { "someone": true },
    });
    const ownerDb = testEnv.authenticatedContext("owner-1").firestore();
    await assertSucceeds(getDoc(doc(ownerDb, "registries", "reg-owner-pending")));
  });

  it("D-19: invitee promoted to invitedUsers (post-accept) CAN read", async () => {
    await seedRegistry("reg-accepted", {
      ownerId: "owner-1",
      visibility: "private",
      invitedUsers: { "accepted-uid": true },
      pendingInvitedUsers: {},
    });
    const acceptedDb = testEnv.authenticatedContext("accepted-uid").firestore();
    await assertSucceeds(getDoc(doc(acceptedDb, "registries", "reg-accepted")));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("popularItems rules — Phase 17 D-43")
//
// popularItems is server-maintained (Admin SDK bypasses rules in the
// onItemCreate/Delete/Update triggers — Plan 17-04). Clients can ONLY read,
// and only when authenticated via a NON-anonymous provider. Anonymous
// web-fallback guests (Phase 5) are excluded per D-12 (Discover is for
// registered users only).
// ─────────────────────────────────────────────────────────────────────────────

describe("popularItems rules (D-43)", () => {
  beforeEach(async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "popularItems", "p1"), {
        canonicalUrl: "https://emag.ro/x",
        title: "Item",
        registryCount: 3,
        registryIds: ["r1", "r2", "r3"],
      });
    });
  });

  it("allows authenticated non-anonymous read", async () => {
    const db = testEnv.authenticatedContext("user1").firestore();
    await assertSucceeds(getDoc(doc(db, "popularItems", "p1")));
  });

  it("denies unauthenticated read", async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, "popularItems", "p1")));
  });

  it("denies anonymous-provider read", async () => {
    const db = testEnv
      .authenticatedContext("anon-user", {
        firebase: { sign_in_provider: "anonymous" },
      })
      .firestore();
    await assertFails(getDoc(doc(db, "popularItems", "p1")));
  });

  it("denies all client writes (even authenticated)", async () => {
    const db = testEnv.authenticatedContext("user1").firestore();
    await assertFails(setDoc(doc(db, "popularItems", "p1"), { registryCount: 999 }));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("discoverCache rules — Phase 17 D-43")
//
// Server-only cache (Cloud Functions Admin SDK). No client read or write.
// ─────────────────────────────────────────────────────────────────────────────

describe("discoverCache rules (D-43)", () => {
  it("denies authenticated read", async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "discoverCache", "q1"), { results: [] });
    });
    const db = testEnv.authenticatedContext("user1").firestore();
    await assertFails(getDoc(doc(db, "discoverCache", "q1")));
  });

  it("denies authenticated write", async () => {
    const db = testEnv.authenticatedContext("user1").firestore();
    await assertFails(setDoc(doc(db, "discoverCache", "q1"), { results: [] }));
  });

  it("denies unauthenticated read + write", async () => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, "discoverCache", "q1")));
    await assertFails(setDoc(doc(db, "discoverCache", "q1"), { results: [] }));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// describe("discoverRateLimits rules — Phase 17 D-43")
//
// Server-only counter (Cloud Functions Admin SDK). Even self-reads are denied
// to prevent any client from enumerating the timestamps array (anti-tamper).
// ─────────────────────────────────────────────────────────────────────────────

describe("discoverRateLimits rules (D-43)", () => {
  it("denies authenticated self read (prevents tampering enumeration)", async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "discoverRateLimits", "u1"), { timestamps: [] });
    });
    const db = testEnv.authenticatedContext("u1").firestore();
    await assertFails(getDoc(doc(db, "discoverRateLimits", "u1")));
  });

  it("denies authenticated self write", async () => {
    const db = testEnv.authenticatedContext("u1").firestore();
    await assertFails(setDoc(doc(db, "discoverRateLimits", "u1"), { timestamps: [] }));
  });

  it("denies cross-user read attempt", async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "discoverRateLimits", "u2"), { timestamps: [] });
    });
    const db = testEnv.authenticatedContext("attacker").firestore();
    await assertFails(getDoc(doc(db, "discoverRateLimits", "u2")));
  });
});
