/**
 * Tests for sendEmail helper — Firestore mail collection write.
 * Task 1, Plan 06-01.
 */

const adds: unknown[] = [];

jest.mock("firebase-admin", () => {
  return {
    __esModule: true,
    initializeApp: jest.fn(),
    firestore: () => ({
      collection: (_path: string) => ({
        add: (data: unknown) => {
          adds.push(data);
          return Promise.resolve({ id: "fake-id" });
        },
      }),
    }),
  };
});

// Must import AFTER jest.mock to pick up the mock
import { sendEmail } from "../email/send";

beforeEach(() => {
  adds.length = 0;
});

describe("sendEmail", () => {
  it("writes one document to mail collection with correct shape", async () => {
    await sendEmail({
      to: "a@b.com",
      subject: "S",
      html: "<p>H</p>",
      text: "T",
    });
    expect(adds).toHaveLength(1);
    expect(adds[0]).toEqual({
      to: "a@b.com",
      message: {
        subject: "S",
        html: "<p>H</p>",
        text: "T",
      },
    });
  });

  it("throws if 'to' is empty string", async () => {
    await expect(
      sendEmail({ to: "", subject: "S", html: "<p></p>", text: "T" })
    ).rejects.toThrow("sendEmail");
  });

  it("throws if 'subject' is empty string", async () => {
    await expect(
      sendEmail({ to: "a@b.com", subject: "", html: "<p></p>", text: "T" })
    ).rejects.toThrow("sendEmail");
  });
});
