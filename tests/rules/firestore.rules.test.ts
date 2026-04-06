import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
  RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc } from "firebase/firestore";
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
