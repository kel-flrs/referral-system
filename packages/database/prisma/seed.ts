import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  console.log('Seeding database...');

  // Clear existing data
  await prisma.referral.deleteMany();
  await prisma.match.deleteMany();
  await prisma.candidateConnection.deleteMany();
  await prisma.consultantActivity.deleteMany();
  await prisma.position.deleteMany();
  await prisma.candidate.deleteMany();
  await prisma.consultant.deleteMany();

  // Create consultants
  const consultant1 = await prisma.consultant.create({
    data: {
      bullhornId: '1001',
      firstName: 'John',
      lastName: 'Smith',
      email: 'john.smith@example.com',
      phone: '555-0101',
      isActive: true,
      totalPlacements: 15,
      totalReferrals: 8,
      lastActivityAt: new Date(),
    },
  });

  const consultant2 = await prisma.consultant.create({
    data: {
      bullhornId: '1002',
      firstName: 'Sarah',
      lastName: 'Johnson',
      email: 'sarah.johnson@example.com',
      phone: '555-0102',
      isActive: true,
      totalPlacements: 22,
      totalReferrals: 12,
      lastActivityAt: new Date(),
    },
  });

  console.log('Created consultants');

  // Create candidates
  const candidate1 = await prisma.candidate.create({
    data: {
      bullhornId: '2001',
      firstName: 'Alice',
      lastName: 'Williams',
      email: 'alice.williams@email.com',
      phone: '555-1001',
      currentTitle: 'Senior Software Engineer',
      currentCompany: 'Tech Corp',
      location: 'San Francisco, CA',
      skills: ['JavaScript', 'TypeScript', 'React', 'Node.js', 'PostgreSQL'],
      summary: 'Experienced full-stack developer with 8 years of experience',
      status: 'ACTIVE',
    },
  });

  const candidate2 = await prisma.candidate.create({
    data: {
      bullhornId: '2002',
      firstName: 'Bob',
      lastName: 'Davis',
      email: 'bob.davis@email.com',
      phone: '555-1002',
      currentTitle: 'Product Manager',
      currentCompany: 'StartupXYZ',
      location: 'New York, NY',
      skills: ['Product Management', 'Agile', 'User Research', 'Data Analysis'],
      summary: 'Product leader with 6 years of experience in B2B SaaS',
      status: 'ACTIVE',
    },
  });

  const candidate3 = await prisma.candidate.create({
    data: {
      bullhornId: '2003',
      firstName: 'Carol',
      lastName: 'Martinez',
      email: 'carol.martinez@email.com',
      phone: '555-1003',
      currentTitle: 'DevOps Engineer',
      currentCompany: 'Cloud Services Inc',
      location: 'Austin, TX',
      skills: ['AWS', 'Docker', 'Kubernetes', 'Terraform', 'Python', 'CI/CD'],
      summary: 'DevOps specialist with expertise in cloud infrastructure',
      status: 'ACTIVE',
    },
  });

  console.log('Created candidates');

  // Create positions
  const position1 = await prisma.position.create({
    data: {
      bullhornId: '3001',
      title: 'Senior Full Stack Developer',
      description: 'Looking for an experienced full-stack developer to join our growing team.',
      employmentType: 'FULL_TIME',
      requiredSkills: ['JavaScript', 'TypeScript', 'React', 'Node.js'],
      preferredSkills: ['PostgreSQL', 'AWS', 'Docker'],
      experienceLevel: 'SENIOR',
      location: 'San Francisco, CA',
      salary: '$150k-$180k',
      clientName: 'Acme Technologies',
      clientBullhornId: '5001',
      status: 'OPEN',
      openDate: new Date(),
    },
  });

  const position2 = await prisma.position.create({
    data: {
      bullhornId: '3002',
      title: 'Product Manager - B2B SaaS',
      description: 'Seeking a strategic product manager for our enterprise SaaS platform.',
      employmentType: 'FULL_TIME',
      requiredSkills: ['Product Management', 'Agile', 'B2B SaaS'],
      preferredSkills: ['User Research', 'Data Analysis', 'SQL'],
      experienceLevel: 'MID',
      location: 'Remote',
      salary: '$130k-$160k',
      clientName: 'Enterprise Solutions Ltd',
      clientBullhornId: '5002',
      status: 'OPEN',
      openDate: new Date(),
    },
  });

  const position3 = await prisma.position.create({
    data: {
      bullhornId: '3003',
      title: 'Cloud Infrastructure Engineer',
      description: 'Join our infrastructure team to build and maintain cloud-native applications.',
      employmentType: 'FULL_TIME',
      requiredSkills: ['AWS', 'Kubernetes', 'Terraform'],
      preferredSkills: ['Docker', 'Python', 'CI/CD', 'Monitoring'],
      experienceLevel: 'MID',
      location: 'Austin, TX',
      salary: '$140k-$170k',
      clientName: 'FinTech Innovations',
      clientBullhornId: '5003',
      status: 'OPEN',
      openDate: new Date(),
    },
  });

  console.log('Created positions');

  // Create some matches
  const match1 = await prisma.match.create({
    data: {
      candidateId: candidate1.id,
      positionId: position1.id,
      overallScore: 92.5,
      skillMatchScore: 95.0,
      experienceScore: 100.0,
      locationScore: 100.0,
      matchedSkills: ['JavaScript', 'TypeScript', 'React', 'Node.js'],
      missingSkills: [],
      matchReason: 'Excellent match with all required skills and strong experience level match. Perfect location alignment.',
      status: 'PENDING',
    },
  });

  const match2 = await prisma.match.create({
    data: {
      candidateId: candidate2.id,
      positionId: position2.id,
      overallScore: 88.0,
      skillMatchScore: 85.0,
      experienceScore: 100.0,
      locationScore: 100.0,
      matchedSkills: ['Product Management', 'Agile', 'User Research', 'Data Analysis'],
      missingSkills: [],
      matchReason: 'Strong match with relevant B2B SaaS experience. Remote position allows for excellent location match.',
      status: 'PENDING',
    },
  });

  const match3 = await prisma.match.create({
    data: {
      candidateId: candidate3.id,
      positionId: position3.id,
      overallScore: 90.0,
      skillMatchScore: 90.0,
      experienceScore: 100.0,
      locationScore: 100.0,
      matchedSkills: ['AWS', 'Kubernetes', 'Terraform', 'Docker', 'Python', 'CI/CD'],
      missingSkills: [],
      matchReason: 'Excellent technical match with all required and preferred skills. Perfect location alignment.',
      status: 'PENDING',
    },
  });

  console.log('Created matches');

  // Create a sample referral
  const referral1 = await prisma.referral.create({
    data: {
      matchId: match1.id,
      candidateId: candidate1.id,
      positionId: position1.id,
      consultantId: consultant1.id,
      referralSource: 'Network connection: LinkedIn',
      referrerName: 'Michael Chen',
      referrerEmail: 'michael.chen@email.com',
      notes: 'Strong recommendation from former colleague. Candidate is actively looking.',
      status: 'PENDING',
    },
  });

  console.log('Created referral');

  // Create candidate connections
  await prisma.candidateConnection.create({
    data: {
      candidateId: candidate1.id,
      connectionType: 'LINKEDIN',
      connectedName: 'Michael Chen',
      connectedEmail: 'michael.chen@email.com',
      connectedTitle: 'Engineering Manager',
      connectedCompany: 'Tech Corp',
      relationshipStrength: 5,
      notes: 'Former colleague and mentor',
    },
  });

  console.log('Created connections');

  console.log('Seed data created successfully!');
}

main()
  .catch((e) => {
    console.error('Error seeding database:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
