import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function cleanupTestData() {
  console.log('\n🧹 Cleaning up test data...');
  console.log('='.repeat(60));

  try {
    await prisma.$transaction([
      prisma.consultant.deleteMany(),
      prisma.candidate.deleteMany(),
      prisma.position.deleteMany(),
      prisma.candidateConnection.deleteMany(),
      prisma.consultantActivity.deleteMany(),
      prisma.job.deleteMany(),
      prisma.match.deleteMany(),
      prisma.referral.deleteMany()
    ]);

    console.log('\n✅ Cleanup complete!');
    console.log('='.repeat(60));
  } catch (error) {
    console.error('\n❌ Error during cleanup:', error);
    throw error;
  }
}

cleanupTestData()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
